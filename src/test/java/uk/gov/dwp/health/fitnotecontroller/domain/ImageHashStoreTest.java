package uk.gov.dwp.health.fitnotecontroller.domain;

import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.time.format.DateTimeFormatter;

import static org.junit.Assert.assertEquals;

public class ImageHashStoreTest {

  private ImageHashStore imageHashStore;

  @Before
  public void setup(){
    imageHashStore = new ImageHashStore();
  }

  @Test
  public void testCreateDateTime(){
    imageHashStore.initCreateDateTime();

    testValidCurrentDate(imageHashStore.getCreateDateTime());
  }

  @Test
  public void testSubmitted(){

    for(int i = 1; i < 10; i++){
      testIncrements(i);
    }

    testValidCurrentDate(imageHashStore.getLastSubmitted());
  }

  private void testValidCurrentDate(String actualDate){
    String expected = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).substring(0, 16);

    assertEquals(expected, actualDate.substring(0, 16));
  }

  private void testIncrements(int increment){
    imageHashStore.incSubmissionCount();

    assertEquals(increment, imageHashStore.getSubmissionCount());

    imageHashStore.updateLastSubmitted();
  }
}
