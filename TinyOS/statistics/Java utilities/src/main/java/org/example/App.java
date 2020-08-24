package org.example;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
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
    public static String TRESHOLDMESSAGE = "Time for treshold " + NUMBER + "message from sink to " + NUMBER + " is : " + NUMBER + " ms";
    public static String SIMULTIME = "current simulation time: "+ NUMBER + ":"+ NUMBER + ":" + NUMBER +"." + NUMBER;

    public static void main(String[] args) {
        BufferedReader reader;
        try {
            File report = new File("C:/Users/ponti/Documents/Github/Middleware-2019-2020/TinyOS/statistics/report.txt");
            report.createNewFile();
            FileWriter writer = new FileWriter("C:/Users/ponti/Documents/Github/Middleware-2019-2020/TinyOS/statistics/report.txt");
            List<Integer> dataTimes = new ArrayList<>();
            List<Integer> tresholdTimes = new ArrayList<>();
            String simulationTime = null;
            List<String> topologies = Arrays.asList("7", "15", "31", "63", "3.40", "5.31");
            for (String topology: topologies){
                dataTimes.clear();
                tresholdTimes.clear();
                writer.write("*** TOPOLOGY " + topology + " ***\n");
                reader = new BufferedReader(new FileReader(
                        "C:/Users/ponti/Documents/Github/Middleware-2019-2020/TinyOS/statistics/topology "+ topology +"/performance.txt"));
                String line = reader.readLine();
                while (line != null) {
                    Pattern pattern = Pattern.compile(DATAMESSAGE);
                    Matcher m = pattern.matcher(line);
                    if (m.find()) {
                        String number = m.group(3);
                        dataTimes.add(Integer.parseInt(number));
                        System.out.println(number);
                    }

                    Pattern pattern1 = Pattern.compile(TRESHOLDMESSAGE);
                    Matcher m1 = pattern1.matcher(line);
                    if (m1.find()) {
                        String number = m1.group(3);
                        tresholdTimes.add(Integer.parseInt(number));
                        System.out.println(number);
                    }

                    Pattern pattern2 = Pattern.compile(SIMULTIME);
                    Matcher m2 = pattern2.matcher(line);
                    if (m2.find()) {
                        simulationTime = m2.group(1) + ":" + m2.group(2) + ":" + m2.group(3) + "." + m2.group(4);
                        System.out.println(simulationTime);
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

                writer.write("****************************************");
                reader.close();
            }



            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

