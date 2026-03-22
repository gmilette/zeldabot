#!/usr/bin/env python3
"""
train_zelda_combat.py — PPO training loop for Zelda KillAll RL policy.

Usage:
    python train_zelda_combat.py               # full 100k-step run
    python train_zelda_combat.py --dry-run 10  # 10 steps for smoke test

Requirements:
    pip install stable-baselines3 gymnasium

Ensure KillAllTrainingServer is running on localhost:5050 before starting.
"""

import argparse
import socket
import struct
import sys
import numpy as np
import gymnasium as gym
from gymnasium.spaces import Box, Discrete
from stable_baselines3 import PPO
from stable_baselines3.common.callbacks import CheckpointCallback


HOST = "localhost"
PORT = 5050

OBS_FLOATS = 113
OBS_BYTES = OBS_FLOATS * 4   # 452
STEP_RESP_BYTES = OBS_BYTES + 4 + 1 + 1  # 458: obs + reward + done + needsDelay

# Actions: NOOP, RIGHT, LEFT, DOWN, UP, A
N_ACTIONS = 6
# Actions: NOOP, RIGHT, LEFT, DOWN, UP, A, B
#N_ACTIONS = 7


# ─── Socket helpers ───────────────────────────────────────────────────────────

def recvall(sock: socket.socket, n: int) -> bytes:
    """Block until exactly n bytes are received."""
    data = bytearray()
    while len(data) < n:
        chunk = sock.recv(n - len(data))
        if not chunk:
            raise ConnectionError("Socket closed before all bytes received")
        data.extend(chunk)
    return bytes(data)


# ─── Gymnasium environment ────────────────────────────────────────────────────

class ZeldaCombatEnv(gym.Env):
    metadata = {"render_modes": []}

    def __init__(self):
        super().__init__()
        self.observation_space = Box(0.0, 1.0, shape=(OBS_FLOATS,), dtype=np.float32)
        self.action_space = Discrete(N_ACTIONS)
        self._sock = None
        self._needs_reset_delay = False
        self._connect()

    def _connect(self):
        if self._sock is not None:
            try:
                self._sock.close()
            except Exception:
                pass
        self._sock = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self._sock.connect((HOST, PORT))
        print(f"[ZeldaCombatEnv] connected to {HOST}:{PORT}")

    RESET_DELAY_BEFORE = 10.0  # seconds to wait before sending RESET
    RESET_DELAY_AFTER = 2.0   # seconds to wait after receiving obs

    def reset(self, *, seed=None, options=None):
        import time
        super().reset(seed=seed)
        if self._needs_reset_delay:
            print(f"[py] reset() early-exit episode — waiting {self.RESET_DELAY_BEFORE}s before...", flush=True)
            time.sleep(self.RESET_DELAY_BEFORE)
        print(f"[py] reset() sending RESET\\n ...", flush=True)
        t0 = time.time()
        self._sock.sendall(b"RESET\n")
        # Read and discard 0x01 keepalive bytes until 0xFF "obs follows" marker.
        # Server sends 0x01 every ~500ms while waiting for the game to settle,
        # then sends 0xFF immediately before the 452-byte obs payload.
        print(f"[py] reset() waiting for 0xFF obs marker...", flush=True)
        b = recvall(self._sock, 1)
        while b[0] != 0xFF:
            if b[0] == 0x01:
                t_ka = time.time() - t0
                print(f"[py] reset() keepalive 0x01 ({t_ka:.1f}s elapsed), still waiting...", flush=True)
            else:
                print(f"[py] reset() unexpected byte {b[0]:#x}, discarding", flush=True)
            b = recvall(self._sock, 1)
        t_marker = time.time() - t0
        print(f"[py] reset() 0xFF marker received in {t_marker:.2f}s — reading {OBS_BYTES} obs bytes...", flush=True)
        raw = recvall(self._sock, OBS_BYTES)
        elapsed = time.time() - t0
        print(f"[py] reset() obs received {len(raw)} bytes in {elapsed:.2f}s", flush=True)
        assert len(raw) == OBS_BYTES, f"Expected {OBS_BYTES} bytes, got {len(raw)}"
        obs = np.frombuffer(raw, dtype=">f4").astype(np.float32)
        assert obs.shape == (OBS_FLOATS,), f"Obs shape mismatch: {obs.shape}"
        if self._needs_reset_delay:
            print(f"[py] reset() sleeping {self.RESET_DELAY_AFTER}s after receive...", flush=True)
            time.sleep(self.RESET_DELAY_AFTER)
        self._needs_reset_delay = False
        print(f"[py] reset() done in {elapsed:.2f}s", flush=True)
        return obs, {}

    def step(self, action: int):
        # Send action as 4-byte big-endian int
        self._sock.sendall(struct.pack(">i", int(action)))

        raw = recvall(self._sock, STEP_RESP_BYTES)
        assert len(raw) == STEP_RESP_BYTES, f"Expected {STEP_RESP_BYTES} bytes, got {len(raw)}"

        obs_raw = raw[:OBS_BYTES]
        reward_raw = raw[OBS_BYTES:OBS_BYTES + 4]
        done_raw = raw[OBS_BYTES + 4:OBS_BYTES + 5]
        delay_raw = raw[OBS_BYTES + 5:OBS_BYTES + 6]

        obs = np.frombuffer(obs_raw, dtype=">f4").astype(np.float32)
        reward = struct.unpack(">f", reward_raw)[0]
        done = bool(done_raw[0])
        needs_delay = bool(delay_raw[0])

        assert obs.shape == (OBS_FLOATS,), f"Obs shape mismatch: {obs.shape}"
        assert not np.isnan(reward), "Reward is NaN"

        if done:
            self._needs_reset_delay = needs_delay
            print(f"[py] step() done=True reward={reward:+.3f} needs_delay={needs_delay}", flush=True)

        return obs, float(reward), done, False, {}

    def close(self):
        if self._sock:
            self._sock.close()
            self._sock = None


# ─── Dry run (smoke test) ─────────────────────────────────────────────────────

def dry_run(n_steps: int):
    print(f"[dry-run] connecting and running {n_steps} steps...")
    env = ZeldaCombatEnv()
    obs, _ = env.reset()
    print(f"[dry-run] reset ok — obs shape={obs.shape}, min={obs.min():.3f}, max={obs.max():.3f}")

    total_reward = 0.0
    for step in range(n_steps):
        action = env.action_space.sample()
        obs, reward, done, _, _ = env.step(action)
        total_reward += reward
        print(f"  step={step+1:3d}  action={action}  reward={reward:+.3f}  done={done}  "
              f"obs_nonzero={np.count_nonzero(obs)}")
        if done:
            print(f"  episode done at step {step+1}, resetting...")
            obs, _ = env.reset()

    print(f"[dry-run] done. total_reward={total_reward:.3f}")
    env.close()


# ─── Training ─────────────────────────────────────────────────────────────────

def train(total_timesteps: int = 100_000, resume_path: str = None):
    env = ZeldaCombatEnv()

    checkpoint_cb = CheckpointCallback(
        save_freq=1_000,
        save_path="./checkpoints/",
        name_prefix="killall_ppo",
    )

    if resume_path:
        print(f"[train] resuming from {resume_path}")
        model = PPO.load(resume_path, env=env, tensorboard_log="./tensorboard/")
        # Recover how many steps were already done from the filename (e.g. killall_ppo_50000_steps.zip)
        import re
        match = re.search(r"_(\d+)_steps", resume_path)
        steps_done = int(match.group(1)) if match else 0
        remaining = max(0, total_timesteps - steps_done)
        print(f"[train] already completed ~{steps_done} steps, running {remaining} more")
    else:
        print(f"[train] starting fresh PPO training for {total_timesteps} timesteps...")
        model = PPO(
            "MlpPolicy",
            env,
            verbose=1,
            tensorboard_log="./tensorboard/",
            policy_kwargs=dict(net_arch=[64, 64]),
            n_steps=512,
            batch_size=64,
            n_epochs=4,
            gamma=0.99,
            learning_rate=3e-4,
        )
        remaining = total_timesteps

    model.learn(total_timesteps=remaining, callback=checkpoint_cb, reset_num_timesteps=not resume_path)

    model.save("killall_ppo_final")
    print("[train] model saved to killall_ppo_final.zip")
    env.close()


# ─── Entry point ─────────────────────────────────────────────────────────────

def main():
    parser = argparse.ArgumentParser(description="Train Zelda KillAll RL policy")
    parser.add_argument("--dry-run", type=int, metavar="N", default=0,
                        help="Run N steps as a smoke test instead of full training")
    parser.add_argument("--timesteps", type=int, default=100_000,
                        help="Total training timesteps (default: 100000)")
    parser.add_argument("--resume", type=str, metavar="PATH", default=None,
                        help="Resume from a checkpoint zip (e.g. checkpoints/killall_ppo_50000_steps.zip)")
    args = parser.parse_args()

    if args.dry_run > 0:
        dry_run(args.dry_run)
    else:
        train(total_timesteps=args.timesteps, resume_path=args.resume)


if __name__ == "__main__":
    main()
