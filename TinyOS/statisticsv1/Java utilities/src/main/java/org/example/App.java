package org.example;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Hello world!
 *
 */
public class App {

    public static String NUMBER = "([0-9]+)";
    public static String DATAMESSAGE = "Time for data message " + NUMBER + " from " + NUMBER + " to sink is : " + NUMBER + " ms";
    public static String TRESHOLDMESSAGE = "Time for treshold " + NUMBER + " message with id " + NUMBER + " from sink to " + NUMBER + " is : " + NUMBER + " ms\\[not discarded\\]";
    public static String DISTRESHOLDMESSAGE =  " from sink to " + NUMBER + " is : " + NUMBER + " ms\\[DISCARDED\\]";
    public static String RADIOBUSY = "radio busy";
    public static String SIMULTIME = "current simulation time: "+ NUMBER + ":"+ NUMBER + ":" + NUMBER +"." + NUMBER;


    public static void main(String[] args) {
        BufferedReader reader;
        try {
            File report = new File("C:/Users/ponti/Github/Middleware-2019-2020/TinyOS/statisticsv1/reportSimple.txt");
            report.createNewFile();
            FileWriter writer = new FileWriter("C:/Users/ponti/Github/Middleware-2019-2020/TinyOS/statisticsv1/reportv1.txt");


            List<String> topologies = Arrays.asList("14 dense", "14 sparse", "random", "mix","tree", "20 dense");
            for (String topology: topologies){
                Integer[] discardedCounter = new Integer[7];
                Integer[] discardedTime = new Integer[7];
                String simulationTime = null;
                for (int i = 0; i< 7; i++){
                    discardedCounter[i] = 0;
                    discardedTime[i] = 0;
                }


                int radiobusy = 0;
                List<Integer> dataTimes = new ArrayList<>();
                List<Integer> tresholdTimes = new ArrayList<>();
                writer.write("***** TOPOLOGY " + topology + " *****\n");
                reader = new BufferedReader(new FileReader(
                        "C:/Users/ponti/Github/Middleware-2019-2020/TinyOS/statisticsv1/topology "+ topology +"/performance.txt"));
                String line = reader.readLine();
                while (line != null) {
                    Pattern pattern = Pattern.compile(DATAMESSAGE);
                    Matcher m = pattern.matcher(line);
                    if (m.find()) {
                        String number = m.group(3);
                        dataTimes.add(Integer.parseInt(number));
                        System.out.println(number + " rrrrrrrrrrrrr");
                    }

                    Pattern pattern1 = Pattern.compile(TRESHOLDMESSAGE);
                    Matcher m1 = pattern1.matcher(line);
                    if (m1.find()) {
                        String number = m1.group(4);
                        tresholdTimes.add(Integer.parseInt(number));
                    }

                    Pattern pattern2 = Pattern.compile(SIMULTIME);
                    Matcher m2 = pattern2.matcher(line);
                    if (m2.find()) {
                        simulationTime = m2.group(1) + ":" + m2.group(2) + ":" + m2.group(3) + "." + m2.group(4);
                        System.out.println(simulationTime);
                    }

                    for (int i = 0; i< 7; i++ ){
                        Integer thresholdId = i +1;
                        Pattern pattern3 = Pattern.compile(String.valueOf(thresholdId) + DISTRESHOLDMESSAGE);
                        Matcher m3 = pattern3.matcher(line);
                        if (m3.find()) {
                            discardedCounter[i]  ++;
                            System.out.println(m3.group(2) + " gig");
                            Integer time  = Integer.parseInt(m3.group(2));
                            if (time > discardedTime[i]){
                                discardedTime[i] = time;
                            }
                            System.out.println(time);
                        }
                    }

                    Pattern pattern4 = Pattern.compile(RADIOBUSY);
                    Matcher m4 = pattern4.matcher(line);
                    if (m4.find()) {
                        radiobusy += 1;
                    }


                    // read next line
                    line = reader.readLine();
                }
                long count = dataTimes.size();
                writer.write("Last recorded simulation time: "+ simulationTime + "\n\n");
                writer.write("Amount of data messages delivered: " + count + "\n");
                int max = dataTimes.stream().max(Integer::compareTo).get();
                writer.write("Maximum time needed to deliver a data message: " + max + "\n");
                int min = dataTimes.stream().min(Integer::compareTo).get();
                writer.write("Minimum time needed to deliver a data message: " + min + "\n");
                double avg = dataTimes.stream().mapToInt( i -> i).average().getAsDouble();
                writer.write("Average time needed to deliver a data message: " + avg + "\n\n");

                count = tresholdTimes.size();
                writer.write("Amount of treshold messages delivered: " + count + "\n");
                max = tresholdTimes.stream().max(Integer::compareTo).get();
                writer.write("Maximum time needed to deliver a treshold message: " + max + "\n");
                min = tresholdTimes.stream().min(Integer::compareTo).get();
                writer.write("Minimum time needed to deliver a treshold message: " + min + "\n");
                avg = tresholdTimes.stream().mapToInt( i -> i).average().getAsDouble();
                writer.write("Average time needed to deliver a treshold message: " + avg + "\n");

                writer.write("DISCARD STATISTICS\n");
                for (int i = 0; i< 7; i++ ){
                    Integer thresholdId = i +1;
                    writer.write("number of discarded messages for Th id " + thresholdId + ": " + discardedCounter[i] + " -- ");
                    writer.write("Last recorded th message liveness time for id " + thresholdId + ": " + discardedTime[i] + "\n");
                }
                avg = Arrays.stream(discardedCounter).filter( (n -> n > 0)).mapToInt( i -> i ).average().getAsDouble();
                writer.write("---Average number of discarded messages: "+ avg + " ---\n");
                avg = Arrays.stream(discardedTime).filter( (n -> n > 0)).mapToInt( i -> i ).average().getAsDouble();
                writer.write("---Average convergence time: "+ avg + " ---\n");
                writer.write("---Number of messages not sent due to a busy radio: "+ radiobusy + " ---\n\n");
                writer.write("****************************************\n");
                reader.close();
            }



            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

