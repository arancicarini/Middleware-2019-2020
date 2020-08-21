## How to build and test the TinyOSProject
- Download Oracle VM VirtualBox for your laptop (make sure to have at least 10 GB of free space on your HDD): https://www.virtualbox.org/wiki/Download_Old_Builds_5_2
- Download the IOT virtual machine from https://tinyurl.com/iotpolimi-vm
- Open VirtualBox and go toFile->Import Appliance and import the file iot_polimi.ova
- Boot the VM: user = user and password = password
- Don’t update the VM, ignore messages
- Clone this repo inside the VM
Open a shell, navigate to TinyOSProject and type:

    - make micaz sim
    
    - python Simulation.py

The script which runs the project is "Simulation.py": here you can specify which topology you want to test from the ones you can find in this folder . Don't forget to change the script according to the number of motes each topology has. Check the output of the debugger in the files simulation.txt and performance.txt
