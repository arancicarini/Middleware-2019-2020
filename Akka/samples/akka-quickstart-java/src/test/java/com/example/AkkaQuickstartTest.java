package com.example;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import org.junit.ClassRule;
import org.junit.Test;

//#definition
public class AkkaQuickstartTest {

    @ClassRule
    public static final TestKitJunitResource testKit = new TestKitJunitResource();
//#definition

    //#test
    @Test
    public void testGreeterActorSendingOfGreeting() {
        TestProbe<Greeter.Command> testProbe = testKit.createTestProbe();
        ActorRef<Greeter.Command> underTest = testKit.spawn(Greeter.create(3), "greeter");
        underTest.tell(new Greeter.Greet("Arianna", testProbe.getRef()));
        testProbe.expectMessage(new Greeter.Greeted("Arianna", underTest));
    }
    //#test
}
