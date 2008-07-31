package org.mobicents.slee.resources.diameter.tests;

import java.io.FileNotFoundException;

import org.junit.Test;
import org.mobicents.slee.resources.diameter.tests.framework.TestingFramework;

public class EventChargingTest
{

  @Test
  public void runTest() throws FileNotFoundException
  {
    TestingFramework tF = new TestingFramework();
    
    tF.runTest( this.getClass().getResourceAsStream( "xml/event-charging-test.xml" ) );
  }
  
}
