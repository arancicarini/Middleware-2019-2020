print "********************************************";
print "*                                          *";
print "*             TOSSIM Script                *";
print "*                                          *";
print "********************************************";

import sys;
import time;

from TOSSIM import*;

t = Tossim([]);


topofile="topology8.txt";
modelfile="meyer-heavy.txt";


print "Initializing mac....";
mac = t.mac();
print "Initializing radio channels....";
radio=t.radio();
print "using topology file:",topofile;
print "using noise file:",modelfile;
print "Initializing simulator....";
t.init();


simulation_outfile = "simulation.txt";
print "Saving sensors simulation output to:", simulation_outfile;
simulation_out = open(simulation_outfile, "w");

debug_outfile = "debug.txt";
print "Saving sensors simulation output to:", debug_outfile;
debug_out = open(debug_outfile, "w");

##Add debug channel
debug_out.write("DEBUG FILE\n\nAdding debug channels:\n\n");
debug_out.write("Activate debug message on channel init\n");
t.addChannel("init",debug_out);
debug_out.write("Activate debug message on channel boot\n");
t.addChannel("boot",debug_out);
debug_out.write("Activate debug message on channel sink\n");
t.addChannel("sink",simulation_out);
t.addChannel("sink",debug_out);
debug_out.write("Activate debug message on channel radio\n");
t.addChannel("radio",debug_out);
debug_out.write("Activate debug message on channel timer\n");
t.addChannel("timer",debug_out);
debug_out.write("Activate debug message on channel data\n");
t.addChannel("data",debug_out);
debug_out.write("Activate debug message on channel error\n");
t.addChannel("error",debug_out);
debug_out.write("Activate debug message on channel treshold\n\n");
t.addChannel("treshold",debug_out);


##Creating nodes
debug_out.write("\nCreating nodes:\n\n");
for i in range(1,8):
	debug_out.write("Creating node: "+str(i)+"\n");
	node=t.getNode(i);
	time=t.ticksPerSecond(); #instant at which each node should be turned on
	node.bootAtTime(time);


##Setting radio channel
debug_out.write("\nCreating radio channels:\n\n");
f = open(topofile, "r");
lines = f.readlines()
for line in lines:
  s = line.split()
  if (len(s) > 0):
    debug_out.write("Setting radio channel from node: "+ str(s[0])+" to node:"+ str(s[1])+",with gain:"+(s[2])+ "dBm\n");
    radio.add(int(s[0]), int(s[1]), float(s[2]))


##Creating channel model
debug_out.write("\nInitializing Closest Pattern Matching (CPM):\n\n");
noise = open(modelfile, "r")
lines = noise.readlines()
compl = 0;
mid_compl = 0;

for line in lines:
    string = line.strip()
    if (string != "") and ( compl < 10000 ):
        val = int(string)
        mid_compl = mid_compl + 1;
        if ( mid_compl > 5000 ):
            compl = compl + mid_compl;
            mid_compl = 0;
            sys.stdout.write ("#")
            sys.stdout.flush()
        for i in range(1, 8):
            t.getNode(i).addNoiseTraceReading(val)

##Creating noise model
for i in range(1, 8):
    debug_out.write("Creating noise model for node:"+str(i)+"\n");
    t.getNode(i).createNoiseModel()

debug_out.write("\nStart simulation with TOSSIM!\n\n");

for i in range(0,1600):
	t.runNextEvent()
	
debug_out.write("\n\nSimulation finished!");

debug_out.close();

