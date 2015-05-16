package com.sn;

import java.io.InputStream;
import java.net.URL;
import java.sql.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

public class TestDriver {

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

        //testReadingURL();
        try {
            testDB();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

     public static final String drive = "oracle.jdbc.driver.OracleDriver";
        /**
         * ���ӵ�ַ�����������ṩ������ס
         * jdbc:oracle:thin:@localhost:1521:ORCL localhost ��ip��ַ��
         */
        public static final String url = "jdbc:oracle:thin:@192.168.0.100:1521:SO";
        /**
         * �û� ����
         */
        public static final String DBUSER="sam";
        public static final String password="sam";


        public static void testDB() throws Exception{
            // TODO Auto-generated method stub
                Connection conn = null;//��ʾ���ݿ�����
                Statement stmt= null;//��ʾ���ݿ�ĸ���
                ResultSet result = null;//��ѯ���ݿ�
                Class.forName(drive);//ʹ��class�������س���
                conn =DriverManager.getConnection(url,DBUSER,password); //�������ݿ�
                //Statement�ӿ�Ҫͨ��connection�ӿ�������ʵ��������
                stmt = conn.createStatement();
                //ִ��SQL�������ѯ���ݿ�
                result =stmt.executeQuery("SELECT 'abc' name FROM dual");
                while (result.next()) {//�ж���û����һ��
                    String name =result.getString(1);
                    System.out.print("name="+name+";");
                    System.out.println();
                }

                //System.out.println(conn);
                result.close();//���ݿ��ȿ����
                stmt.close();
                conn.close();//�ر����ݿ�

        }
     static void testReadingURL()
     {
         String str;
            try {
            URL url = new URL("http://hq.sinajs.cn/list=sh601318,sh601313");
                InputStream is = url.openStream();
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                while ((str = br.readLine()) != null)
                {
                    System.out.println("haha");
                    System.out.println(str);
                }
                br.close();
            } catch(IOException e) {
                System.out.println(e);
            }
     }
}
