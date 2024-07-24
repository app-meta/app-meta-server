package org.appmeta.tool;
/*
 * @project app-meta-server
 * @file    org.appmeta.tool.OSToolJTest
 * CREATE   2024年07月08日 18:30 下午
 * --------------------------------------------------------------
 * 0604hx   https://github.com/0604hx
 * --------------------------------------------------------------
 */

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class OSToolJTest {

    @Test
    public void byPort(){
        String port = "8000";
        String line;
        try {
            ProcessBuilder builder = new ProcessBuilder("netstat", "-ano", "|", "findstr", String.format(":%s", port));
            Process process = builder.redirectErrorStream(true).start();

            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((line = input.readLine()) != null) {
                System.out.println(line);
                if (line.contains(String.format(":%s", port))) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 4) {
                        System.out.println("PID: " + parts[1]);
                    }
                }
            }
            input.close();
        }
        catch (Exception e){
            System.out.println("_______________________________");
            e.printStackTrace();
        }
    }
}
