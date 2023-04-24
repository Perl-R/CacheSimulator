import os
import subprocess
from collections import OrderedDict

subprocess.run(["make"])

testCommands = {
    "validation0.txt" : "java sim_cache gcc_trace.txt 16 1024 2 0 0 0 0",
    "validation1.txt" : "java sim_cache perl_trace.txt 16 1024 1 0 0 0 0",
    "validation2.txt" : "java sim_cache gcc_trace.txt 16 1024 2 0 0 1 0",
    "validation3.txt" : "java sim_cache vortex_trace.txt 16 1024 2 0 0 2 0",
    "validation4.txt" : "java sim_cache gcc_trace.txt 16 1024 2 8192 4 0 0",
    "validation5.txt" : "java sim_cache go_trace.txt 16 1024 1 8192 4 0 0",
    "validation6.txt" : "java sim_cache gcc_trace.txt 16 1024 2 8192 4 0 1",
    "validation7.txt" : "java sim_cache compress_trace.txt 16 1024 1 8192 4 0 1"
}

numCorrect = 0
for validationFile, command in testCommands.items():

    command = command.split(" ")
    print(command)
    output = subprocess.run(command, text=True)
    
    with open('temp.txt', 'w') as f:
        output = subprocess.run(command, stdout=f)
    
    output = subprocess.run(["diff", "./temp.txt", f"./validation_runs/{validationFile}", "-iw"], stdout=subprocess.PIPE)

    if len(output.stdout) != 0:
        print(f"Incorrect output for {validationFile}")
    else:
        numCorrect += 1

    os.remove("temp.txt")

print(f"Number Correct: {numCorrect} / 8")
subprocess.run(["make", "clean"])
