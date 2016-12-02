import sys,os,math,subprocess

repeats = 10
Participants = [100, 200, 400, 800, 1600, 3200, 6400]
Friends = [0, 0.25, 0.5, 0.75]
Players = ["g1","g2","g3","g4","g5","g6","g7","g8","g9"]

if (len(sys.argv) > 1):
	player = sys.argv[1]

for d in Participants:
    for fr in Friends:
        f = int(math.floor(fr * d))
        for player in Players:
            for i in range(repeats):
                command = "java sqdance.sim.Simulator -g " + player + " -f " + str(f) + " -d " + str(d) + " 2>> " + "results.csv"
                print command
                os.system(command)
                
