import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import java.sql.*;

/**
 * Main Class to controll the program flow
 */
public class ServiceModule 
{
    
    static int serverPort = 7005;
    static int numServerCores = 2 ;
    //------------ Main----------------------
    public static void main(String[] args) throws IOException 
    {           
        // Creating a thread pool
        ExecutorService executorService = Executors.newFixedThreadPool(numServerCores);
        
        //Creating a server socket to listen for clients
        ServerSocket serverSocket = new ServerSocket(serverPort); //need to close the port
        Socket socketConnection = null;
        
        // Always-ON server
        while(true)
        {
            System.out.println("Listening port : " + serverPort 
                                + "\nWaiting for clients...");
            socketConnection = serverSocket.accept();   // Accept a connection from a client
            System.out.println("Accepted client :" 
                                + socketConnection.getRemoteSocketAddress().toString() 
                                + "\n");
            //  Create a runnable task
            Runnable runnableTask = new QueryRunner(socketConnection);
            //  Submit task for execution   
            executorService.submit(runnableTask);   
        }
    }
}


class QueryRunner implements Runnable
{
    //  Declare socket for client access
    protected Socket socketConnection;

    public QueryRunner(Socket clientSocket)
    {
        this.socketConnection =  clientSocket;
    }

    public void run()
    {
        
      try
        {
            //  Reading data from client
            InputStreamReader inputStream = new InputStreamReader(socketConnection
                                                                  .getInputStream()) ;
            BufferedReader bufferedInput = new BufferedReader(inputStream) ;
            OutputStreamWriter outputStream = new OutputStreamWriter(socketConnection
                                                                     .getOutputStream()) ;
            BufferedWriter bufferedOutput = new             BufferedWriter(outputStream) ;
            PrintWriter printWriter = new PrintWriter(bufferedOutput, true) ;
            
            String clientCommand = "" ;
            String responseQuery = "" ;
            String queryInput = "" ;

            while(true)
            {
                // Read client query
                clientCommand = bufferedInput.readLine();
                // System.out.println("Recieved data <" + clientCommand + "> from client : " 
                //                     + socketConnection.getRemoteSocketAddress().toString());

                //  Tokenize here
                StringTokenizer tokenizer = new StringTokenizer(clientCommand);
                queryInput = tokenizer.nextToken();

                if(queryInput.equals("#"))
                {
                    printWriter.println("#");
                    String returnMsg = "Connection Terminated - client : " 
                                        + socketConnection.getRemoteSocketAddress().toString();
                    System.out.println(returnMsg);
                    inputStream.close();
                    bufferedInput.close();
                    outputStream.close();
                    bufferedOutput.close();
                    printWriter.close();
                    socketConnection.close();
                    return;
                }

                //-------------- extracting passenger info ------------------------
                int pass_num = Integer.parseInt(queryInput);
                String pass_name[] = new String[pass_num];
                String pass_name_str = "";

                for (int i=0 ; i<pass_num ; i++) {
                    queryInput = tokenizer.nextToken();
                    if (i!=pass_num-1){
                        queryInput = queryInput.substring(0,queryInput.length()-1);
                    }
                    pass_name[i] = queryInput;
                    pass_name_str = pass_name_str + queryInput;
                    if (i!=pass_num-1){
                        pass_name_str = pass_name_str + ",";
                    }
                }
                String train_no = queryInput = tokenizer.nextToken();
                String date = queryInput = tokenizer.nextToken();
                String coach_type = queryInput = tokenizer.nextToken();

                // for (int i=0 ; i<pass_num ; i++)
                //     System.out.println(pass_name[i]);
                // System.out.println(train_no);
                // System.out.println(date);
                // System.out.println(coach_type);

                //-----------------------------------------------------------------


                //-------------- your DB code goes here----------------------------

                // responseQuery = "******* Dummy result ******";
                // System.out.println("1");
                Connection c = null;

                try {
                    Class.forName("org.postgresql.Driver");

                    String server = "localhost";
                    String database = "db_proj";
                    String port = "5432";
                    String username = "postgres";
                    String password = "yash";

                    c = DriverManager.getConnection("jdbc:postgresql://" + server 
                                                    + ":" + port 
                                                    + "/" + database, 
                                                    username, 
                                                    password);

                    
                    String time1 = Long.toString(System.currentTimeMillis());
                    // train_no = train_no.substring(0,4);
                    time1 = time1.substring(time1.length()-1,time1.length());
                    // System.out.println(time1);

                    String pnr = train_no + date.substring(0,4) + date.substring(5,7) + date.substring(8,10) ;
                    // System.out.println(pnr);

                    String query = "select * from book_ticket("+
                        Integer.toString(pass_num)+"::int,"+  
                        "'{"+pass_name_str+"}'::text[],"+
                        train_no+","+  
                        "'"+date+"'::date,"+ 
                        "'"+coach_type+"'::text," + 
                        pnr+"::bigint);";
                        // System.out.println(query);

                    try {
                        Statement stmt = c.createStatement();
                        ResultSet rs = stmt.executeQuery(query);
                        String status = "";
                        System.out.println(status);
                        while (rs.next()) {
                            status = rs.getString("status__");
                            pnr = rs.getString("pnr__");
                        }
                        if (status.equals("-1")){
                            responseQuery = "TNA -- ";
                            responseQuery = responseQuery + "PNR: " + pnr;
                        }
                        else if (status.equals("-2")){
                            responseQuery = "SNA -- ";
                            responseQuery = responseQuery + "PNR: " + pnr;
                        }
                        else{
                            query = "select * from ticket_pass T where T.pnr="+pnr+";";
                            stmt = c.createStatement();
                            rs = stmt.executeQuery(query);
                            responseQuery = "CNF -- ";
                            responseQuery = responseQuery + "PNR: " + pnr;
                            responseQuery = responseQuery + " ; TRAIN_NO: " + train_no;
                            responseQuery = responseQuery + " ; DATE: " + date;
                            responseQuery = responseQuery + " ; COACH_TYPE: " + coach_type;
                            while (rs.next()) {
                                String name = rs.getString("name");
                                String coach_no = rs.getString("coach_no");
                                String berth_no = rs.getString("berth_no");
                                responseQuery = responseQuery + " ; " + name + " - " + coach_type.substring(0,1) + coach_no + "/" + berth_no;
                            }
                        }
                        pnr = "";
                        
                    } catch (Exception e) {
                        System.out.println(e);
                    }
                    c.close();


                } catch (Exception e) {
                    System.out.println(e);
                }

               

                //----------------------------------------------------------------
                
                //  Sending data back to the client
                printWriter.println(responseQuery); 
                // System.out.println("\nSent results to client - " 
                //                     + socketConnection.getRemoteSocketAddress().toString() );
                
            }
        }
        catch(IOException e)
        {
            return;
        }
    }
}

// java -cp .;org.jdbc_driver.jar ServiceModule.java