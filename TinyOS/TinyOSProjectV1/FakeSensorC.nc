/**
 *  Configuration file for wiring FakeSensorP module to other
 *  components to simulate the behavior of a real sensor
 *
 */
 
generic configuration FakeSensorC() {

	provides interface Read<uint32_t>;

} implementation {

	components MainC, RandomC;
	components new FakeSensorP();
	components new TimerMilliC();
	
	//Connects the provided interface
	Read = FakeSensorP;
	
	//Random interface and its initialization	
	FakeSensorP.Random -> RandomC;
	RandomC <- MainC.SoftwareInit;
	
	//Timer interface	
	FakeSensorP.Timer0 -> TimerMilliC;

}
