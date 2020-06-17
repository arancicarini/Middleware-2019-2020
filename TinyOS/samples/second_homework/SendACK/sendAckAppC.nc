/**
 *  Configuration file for wiring of sendAckC module to other common 
 *  components needed for proper functioning
 *
 *  @author Luca Pietro Borsani
 */

#include "sendAck.h"

configuration sendAckAppC {}

implementation {


/****** COMPONENTS *****/
  components MainC, sendAckC as App;
  components new TimerMilliC() as Timer;
  components new FakeSensorC();
  components ActiveMessageC;
  components new AMSenderC( AM_MY_MSG);
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
  App.Ack -> ActiveMessageC;
  
  //Interfaces to access package fields
  App.AMPacket -> AMSenderC;
  App.Packet -> AMSenderC;
  
  //Timer interface
  App.MilliTimer -> Timer;  
  
  //Fake Sensor read
  App.Read -> FakeSensorC;

}

