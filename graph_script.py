# Script to obtain information needed for each graph
import os
import subprocess

# Compile
subprocess.run(["make"])

# Template
command_template = 'java sim_cache {trace} {block_size} {l1_size} {l1_assoc} {l2_size} {l2_assoc} {policy} {inclusion}'

# # Graph 1 (55 simulations)
print("Graph 1,2 Simulations")

L1_sizes = [2**i for i in range(10,21)]
L1_assocs = [2**i for i in range(0,4)]

path = r"./Graph1_Results/"
os.makedirs(path, exist_ok=True)
for size in L1_sizes:
    for assoc in L1_assocs:
        # Changing info
        command = command_template.replace("{l1_size}", f"{size}")        
        command = command.replace("{l1_assoc}", f"{assoc}")

        # Stay the same
        command = command.replace("{block_size}", "32")
        command = command.replace("{trace}", "gcc_trace.txt")
        command = command.replace("{l2_assoc}", "0")
        command = command.replace("{l2_size}", "0")
        command = command.replace("{policy}", "0")
        command = command.replace("{inclusion}", "0")

        command = command.split(" ")
        print(command)

        with open(os.path.join(path, f"output_{size}_{assoc}.txt"), 'w') as f:
            output = subprocess.run(command, stdout=f)
    
    # fully associative case:
    # num_sets = 1 = Size / (Assoc * Blocksize)
    # Assoc = Size / Blocksize
    fully_assoc = size // 32

    # Changing info
    command = command_template.replace("{l1_size}", f"{size}")        
    command = command.replace("{l1_assoc}", f"{fully_assoc}")

    # Stay the same
    command = command.replace("{block_size}", "32")
    command = command.replace("{trace}", "gcc_trace.txt")
    command = command.replace("{l2_assoc}", "0")
    command = command.replace("{l2_size}", "0")
    command = command.replace("{policy}", "0")
    command = command.replace("{inclusion}", "0")
    command = command.split(" ")

    print(command)
    with open(os.path.join(path, f"output_{size}_{fully_assoc}_fully.txt"), 'w') as f:
        output = subprocess.run(command, stdout=f)


# Graph 3: Replacement Policy Study
print("Graph 3 Simulations")

policies = range(0, 7)
L1_sizes = [2**i for i in range(10,19)]

path = r"./Graph3_Results/"
os.makedirs(path, exist_ok=True)
for policy in policies:
    for size in L1_sizes:

        # Changing
        command = command_template.replace("{policy}", f"{policy}")
        command = command.replace("{l1_size}", f"{size}")        

        # Stays the same
        command = command.replace("{inclusion}", "0")
        command = command.replace("{l2_size}", "0")
        command = command.replace("{l2_assoc}", "0")
        command = command.replace("{l1_assoc}", "4")
        command = command.replace("{block_size}", "32")
        command = command.replace("{trace}", "gcc_trace.txt")
        
        command = command.split(" ")
        print(command)

        with open(os.path.join(path, f"output_{policy}_{size}.txt"), 'w') as f:
            output = subprocess.run(command, stdout=f)



# Graph 4: Inclusivity Testing
print("Graph 4 Simulations")

l2_sizes = [2**i for i in range(11, 17)]
inclusivities = [0, 1]

path = r"./Graph4_Results/"
os.makedirs(path, exist_ok=True)
for inclusion in inclusivities:
    for size in l2_sizes:
        # Changing
        command = command_template.replace("{inclusion}", f"{inclusion}")
        command = command.replace("{l2_size}", f"{size}")

        # Stays the same
        command = command.replace("{l1_size}", "1024")        
        command = command.replace("{l1_assoc}", "4")
        command = command.replace("{block_size}", "32")
        command = command.replace("{trace}", "gcc_trace.txt")
        command = command.replace("{l2_assoc}", "8")
        command = command.replace("{policy}", "0")

        command = command.split(" ")
        print(command)
        
        with open(os.path.join(path, f"output_{inclusion}_{size}.txt"), 'w') as f:
            output = subprocess.run(command, stdout=f)

subprocess.run(["make", "clean"])
