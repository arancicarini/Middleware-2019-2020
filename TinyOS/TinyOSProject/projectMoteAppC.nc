#include "project.h"

configuration projectMoteAppC {}

implementation {


	/****** COMPONENTS *****/
	components MainC, projectMoteC as App;
	components new TimerMilliC() as Timer;
	components new FakeSensorC();
	components ActiveMessageC;
	components new AMSenderC(AM_MY_MSG);
	components new AMReceiverC (AM_MY_MSG);

	/****** INTERFACES *****/
	
	//Boot interface
	App.Boot -> MainC.Boot;

	/****** Wire the other interfaces down here *****/

	//Send and Receive interfaces
	App.AMSend-> AMSenderC;
	App.Receive-> AMReceiverC;

	//Radio Control
	App.SplitControl -> ActiveMessageC;

	//Interfaces to access package fields
	App.AMPacket -> AMSenderC;
	App.Packet -> AMSenderC;

	//Timer interface
	App.MilliTimer -> Timer;  

	//Fake Sensor read
	App.Read -> FakeSensorC;

}

