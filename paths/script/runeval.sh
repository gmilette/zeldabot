SET label = $1
# check the trails first, if trails already contains this run then skip
# boomerang is not helpful in 2_29 because it slows down beating the level
./gradlew clean run --args="room_2_79_d_d dev label=$1 trials=30 hearts=16 shield"
./gradlew clean run --args="room_8_94_m_b dev label=$1 trials=30 hearts=16 shield"
./gradlew clean run --args="room_8_62_m_b dev label=$1 trials=30 hearts=16 shield"
# level 9
# level 7,
# level 6,
# level 4,
# level 5, mummies.. test to see if boomerang is helpful here where there are no projectiles
./gradlew clean run --args="room_5_101 dev label=$1 trials=30 hearts=16 shield"
# level 1, bats (for speed)
# overworld, near lev 3
# overworld, sword shooting guys