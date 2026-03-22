import torch
import numpy as np
from stable_baselines3 import PPO

MODEL_PATH = "killall_ppo_final"
ONNX_PATH = "killall_policy.onnx"
OBS_SIZE = 113

print(f"Loading SB3 model from {MODEL_PATH}...")
model = PPO.load(MODEL_PATH)
policy = model.policy
policy.eval()

dummy_input = torch.zeros(1, OBS_SIZE)

print(f"Exporting to {ONNX_PATH}...")
torch.onnx.export(
    policy,
    dummy_input,
    ONNX_PATH,
    input_names=["obs"],
    output_names=["action"],
    opset_version=17,
    dynamic_axes={"obs": {0: "batch"}, "action": {0: "batch"}}
)
print(f"Done — {ONNX_PATH}")
