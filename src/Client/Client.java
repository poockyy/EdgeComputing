/**
 * Author: Seth Wolfgang
 * Date: 4/19/2022
 * This program serves as the client/gatherer of the network.
 * It sends an image from the `middle` layer
 */

package Client;

import org.apache.commons.io.FileUtils;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Client {

    //Initial vars
    easyFTPClient ftpClient;
    String size;
    Socket socket;
    String address;

    // constructor to put ip address, port, test, and iterations.
    public Client(String address, int ftpPort) throws IOException {
        this.address = address;
        socket = new Socket();
        InetSocketAddress edgeServerSocketAddress = new InetSocketAddress(this.address, 5001);

        //connects to edge server
        connectToEdgeServer(edgeServerSocketAddress);

        //Setup before connection occurs
        ftpClient = new easyFTPClient(address, ftpPort);
        System.out.println("FTP:Client connected to edge server");
        File copiedFile = null;
        File file = null;

        //sets the parameters for what the client does
        int[] configData = receiveParameters();
        int test = configData[0];
        int size = configData[1];
        int iterations = configData[2];
        int ID = configData[3];

        //for size parameter -> used in input file names
        switch (size) {
            case 1 -> this.size = "Small";
            case 2 -> this.size = "Medium";
            case 3 -> this.size = "Large";
        }

        switch (test) {
            case 1: //OCR Test
                //sending image file
                for (int i = 0; i < iterations; i++) {
                    copiedFile = new File("ftpResources" + File.separator + "images" + File.separator + "woahman" + this.size + i + "C" + ID + ".png");
                    file = new File("ftpResources" + File.separator + "images" + File.separator + "woahman" + this.size + ".png");
                    copyAndSendFile(file, copiedFile);
                }
                break; //end of OCR test

            case 2: //Smith-Waterman Test
                String[] SWinputFiles = {"query", "database", "alphabet", "scoringmatrix"};

                for (int i = 0; i < iterations; i++) {
                    for (int j = 0; j < SWinputFiles.length; j++) {
                        copiedFile = new File("ftpResources" + File.separator + "SW" + File.separator + "" + SWinputFiles[j] + this.size + i + "C" + ID + ".txt");
                        file = new File("ftpResources" + File.separator + "SW" + File.separator + "" + SWinputFiles[j] + this.size + ".txt");
                        copyAndSendFile(file, copiedFile);
                    }//end of j loop
                }//end of i loop
                break; //End of Smith-Waterman test

            case 3: //logistic regression
                String[] LGInputFiles = {"BreastCancer", "testData"};

                for (int i = 0; i < iterations; i++) {
                    for (int j = 0; j < 2; j++) {
                        copiedFile = new File("ftpResources" + File.separator + "LogRegression" + File.separator + "" + LGInputFiles[j] + this.size + i + "C" + ID + ".txt");
                        file = new File("ftpResources" + File.separator + "LogRegression" + File.separator + "" + LGInputFiles[j] + this.size + ".txt");
                        copyAndSendFile(file, copiedFile);
                    }
                }
                break; //end of logistic regression

        }//end of switch
        this.ftpClient.closeConnection();
        System.out.println("Client disconnected from FTP server");
    }

    /**
     * Method to copy and send a file. Created to reduce unnecessary lines of code
     *
     * @param file
     * @param copiedFile
     */

    private void copyAndSendFile(File file, File copiedFile) {
        try {
            FileUtils.copyFile(file, copiedFile);
            this.ftpClient.sendFile(copiedFile);
            copiedFile.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method allows the client to receive the parameters of what
     * is sent and how many times to send a file to the edge server.
     * Data comes in a format of
     * test + ";" + size + ";" + iterations + ";" + clientNum
     *
     * @return
     * @throws IOException
     */

    private int[] receiveParameters() throws IOException {
        DataInputStream dataInputStream = new DataInputStream(this.socket.getInputStream());
        int[] configData = new int[4];
        String[] configDataString = new String[4];
        boolean messageReceived = false;

        //formats data from edge server
        while(!messageReceived){
            configDataString = dataInputStream.readUTF().split(";");
            messageReceived = true;
        }

        //parses the data sent from the edge server
        for(int i = 0; i < configDataString.length; i++){
            configData[i] = Integer.parseInt(configDataString[i]);
        }

        return configData;
    }


    /**
     * this method allows for redundancy in connecting to the edge server.
     * The client may fail to connect an attempt to connect at startup.
     *
     * @param edgeServerSocketAddress
     */

    private void connectToEdgeServer(InetSocketAddress edgeServerSocketAddress){
        while (!socket.isConnected()) {
            try {
                socket = new Socket();
                socket.connect(edgeServerSocketAddress); // try the connection
            } catch (IOException e) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                }
            }
        }
    }

}    // end of client
