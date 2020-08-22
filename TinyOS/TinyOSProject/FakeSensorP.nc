/**
 *  Source file of the module of the fake sensor component used to test the middleware application logic in TOSSIM.
 *
 */
 
generic module FakeSensorP() {

	provides interface Read<uint32_t>;
	
	uses interface Random;
	uses interface Timer<TMilli> as Timer0;

} implementation {

	//***************** Boot interface ********************//
	command error_t Read.read(){
		call Timer0.startOneShot( 10 );
		return SUCCESS;
	}

	//***************** Timer0 interface ********************//
	event void Timer0.fired() {
		signal Read.readDone( SUCCESS, call Random.rand32() );
	}
}
