/*
The MIT License (MIT)

Copyright (c) Terry Evans Vaughn "VCSCode"

All rights reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package com.blueseer.edi;

import static com.blueseer.edi.EDIMap.ed;
import com.blueseer.utl.OVData;
import com.blueseer.utl.BlueSeerUtils;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;


/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author vaughnte
 */
public class EDI {
    
    // controlarray in this order : senderid, doctype, map, filename, isacontrolnum, gsctrlnum, stctrlnum, ref, outfile
    // outfile is an optional parameter passed during cmdline processing
    
  //  public static String[] controlarray = new String[21];
    
    public static String[] initEDIControl() {   
        String[] controlarray = new String[23];
             /*  22 elements consisting of:
            c[0] = senderid;
            c[1] = doctype;
            c[2] = map;
            c[3] = infile;
            c[4] = controlnum;
            c[5] = gsctrlnbr;
            c[6] = docid;
            c[7] = ref;
            c[8] = outfile;
            c[9] = sd;
            c[10] = ed;
            c[11] = ud;
            c[12] = overideenvelope
            c[13] = isastring
            c[14] = gsstring
            c[15] = dir
            c[16] = idxnbr
            c[17] = isastart
            c[18] = isaend
            c[19] = docstart
            c[20] = docend
            c[21] = receiverid;
            c[22] = error handling delimited field separated by ;
              */
               for (int i = 0; i < 23; i++) {
                    controlarray[i] = "";
                }
                return controlarray;
    }
   
    public static String[] processFile(String infile, String map, String outfile, String isOverride) throws FileNotFoundException, IOException, ClassNotFoundException {
        
        
        String[] m = new String[]{"0",""};
        String[] c = null;  // control values to pass to map and log
        File file = new File(infile);
        BufferedReader f = new BufferedReader(new FileReader(file));
         char[] cbuf = new char[(int) file.length()];
         f.read(cbuf); 
         f.close();
         
         // now lets see how many ISAs and STs within those ISAs and write character positions of each
         Map<Integer, Object[]> ISAmap = new HashMap<Integer, Object[]>();
         int start = 0;
         int end = 0;
         int isacount = 0;
         int isactrl = 0;
         int gscount = 0;
         int stcount = 0;
         int ststart = 0;
         int sestart = 0;
         String ed_escape = "";
         String sd_escape = "";
         int gsstart = 0;
         String doctype = "";
         String docid = "";
         ArrayList<String> isaList = new ArrayList<String>();
          
          char e = 0;
          char s = 0;
          char u = 0;
          
          
           
           Map<Integer, ArrayList> stse_hash = new HashMap<Integer, ArrayList>();
           ArrayList<Object> docs = new ArrayList<Object>();
          
            for (int i = 0; i < cbuf.length; i++) {
                
                if (cbuf[i] == 'I' && cbuf[i+1] == 'S' && cbuf[i+2] == 'A') {
                    e = cbuf[i+103];
                    u = cbuf[i+104];
                    s = cbuf[i+105];
                    // lets bale if not proper ISA envelope.....unless the 106 is carriage return...then ok
                    if (cbuf[i+106] != 'G' && cbuf[i+107] != 'S' && ! String.format("%02x",(int) cbuf[i+106]).equals("0a")) {
                        return m = new String[]{"1","malformed envelope"};
                    }
                    ed_escape = escapeDelimiter(String.valueOf(e));
                    sd_escape = escapeDelimiter(String.valueOf(s));
                    if (String.format("%02x",(int) cbuf[i+105]).equals("0d") && String.format("%02x",(int) cbuf[i+106]).equals("0a"))
                        s = cbuf[i+106];
                    start = i;
                    isaList.add("ISA" + ":" + String.valueOf(i) + ":" + String.format("%02x",(int) s) );
                    isacount++;
                    String[] isa = new String(cbuf, i, 105).split(ed_escape);
                    isactrl = Integer.valueOf(isa[13]);
                    
                     // set control
                    c = initEDIControl();
                    c[0] = isa[6].trim(); // senderid
                    c[21] = isa[8].trim(); // receiverid
                    c[2] = map;
                    c[3] = infile;
                    c[4] = isa[13]; //isactrlnbr
                    c[8] = outfile;
                    c[9] = String.valueOf((int) s);
                    c[10] = String.valueOf((int) e);
                    c[11] = String.valueOf((int) u);
                    c[12] = isOverride;
                    c[13] = new String(cbuf,i,105);
                    c[15] = "0"; // inbound
                    
                }
                if (i > 1 && cbuf[i-1] == s && cbuf[i] == 'G' && cbuf[i+1] == 'S') {
                    gscount++;
                    isaList.add("GS" + ":" + String.valueOf(i));
                    gsstart = i;
                    String[] gs = new String(cbuf, gsstart, 90).split(ed_escape);
                    c[5] = gs[6]; // gsctrlnbr
                    gs[8] = gs[8].split(sd_escape)[0];
                    c[14] = String.join(String.valueOf(e), Arrays.copyOfRange(gs, 0, 9));
                    
                }
                if (i > 1 && cbuf[i-1] == s && cbuf[i] == 'S' && cbuf[i+1] == 'T') {
                   
                    stcount++;
                    isaList.add("ST" + ":" + String.valueOf(i));
                    ststart = i;
                    
                    String[] st = new String(cbuf, i, 16).split(ed_escape);
                    doctype = st[1]; // doctype
                    docid = st[2].split(sd_escape)[0]; //docID  // to separate 2nd element of ST because grabbing 16 characters in buffer
                   
                   // System.out.println(c[0] + "/" + c[1] + "/" + c[4] + "/" + c[5]);
                } 
                if (i > 1 && cbuf[i-1] == s && cbuf[i] == 'S' && cbuf[i+1] == 'E') {
                    isaList.add("SE" + ":" + String.valueOf(i));
                    sestart = i;
                    // add to hash if hash doesn't exist or insert into hash
                    docs.add(new Object[] {new Integer[] {ststart, sestart}, doctype, docid});
                    // painful reminder that you have to create copy of array at instance in time
                    ArrayList copydocs = new ArrayList(docs);
                    stse_hash.put(isacount, copydocs);
                }
                if (i > 1 && cbuf[i-1] == s && cbuf[i] == 'I' && cbuf[i+1] == 'E' && cbuf[i+2] == 'A') {
                    end = i + 14 + Integer.valueOf(String.valueOf(gscount).length()) + 1;
                    // now add to ISAmap
                   HashMap<Integer,ArrayList> mycopy = new HashMap<Integer,ArrayList>(stse_hash);
                  ISAmap.put(isacount, new Object[] {start, end, (int) s, (int) e, (int) u, mycopy, c});
                    isaList.add("IEA" + ":" + String.valueOf(i));
                    stcount = 0;
                    docs.clear();
                    stse_hash.remove(isacount);
                    
                } 
            }
       
            
         
         /* lets check to see if its x12 or edifact or csv or xml */
         /* if type comes back as empty...then file will slip through unnoticed..probably need to come back to this with a finally */
         String editype = getEDIType(cbuf, infile);
         String batchfile = "";
      //   if (editype.equals("EDIFACT")) {
     //        processEDIFACT(cbuf, filename);
     //    }
         
         if (editype.equals("X12")) {
            // create batch file and assign to control replacing infile name
            
             if (isOverride.isEmpty()) {
             int filenumber = OVData.getNextNbr("edifile");
             batchfile = "R" + String.format("%07d", filenumber);
             processX12(ISAmap, cbuf, batchfile);
             } else {
              processX12(ISAmap, cbuf, infile);   
             }
             
         }
         
     //    if (editype.equals("CSV")) {
    //         processCSV(cbuf, filename);
    //     }
         
   //      if (editype.equals("XML")) {
    //         processXML(file, filename);
    //     }
         
        // if type is unknown then bail....otherwise create batch file of infile
         if (editype.isEmpty()) {
           System.out.println("Unknown file type");
           m = new String[]{"0","Unknown file type"};
         } else {
             if (isOverride.isEmpty()) {
             Files.copy(file.toPath(), new File(OVData.getEDIBatchDir() + "/" + batchfile).toPath());
             }
         }
         
       return m;  
    }
    
    public static String getEDIType(char[] cbuf, String filename) {
    String type = "";
    String[] filenamesplit = null;
        
     // identification rules for csv and xml are based on filename convention
     // identification of x12 and EDIFACT are based on first three chars of file content
    
    // filename check for csv or xml 
    // filename convention must be tpid.uniquewhatever.csv or tpid.uniquewhatever.xml
    if (filename.toString().toUpperCase().endsWith(".CSV") || filename.toString().toUpperCase().endsWith(".XML") ) {
        filenamesplit = filename.split("\\.", -1);
        if (filenamesplit.length >= 3 ) {
                          type = filenamesplit[filenamesplit.length - 1].toString().toUpperCase();
        } else {
            type = "";
        }
    }
    
    // otherwise filename can be anything....so assume EDI x12 or Edifact
    // look at first three characters of file content
    StringBuilder sb = new StringBuilder();
         sb.append(cbuf, 0, 3);
         if (sb.toString().equals("ISA"))
             type = "X12";
         if (sb.toString().equals("UNB"))
             type = "EDIFACT";
    return type;
}
    
    public static void processXML(File file, String filename)   {
    ArrayList<String> doc = new ArrayList<String>();
    String[] filenamesplit = null;
        String doctype = "XML";
        String controlnum = "-1";
        String docid = "-1";
        String map = "";
        String senderid = "";
        String[] control= initEDIControl();
        boolean proceed = true;
    //lets first get the tpid from the filename convention.....
    // filename convention must be tpid.uniquewhatever.csv or tpid.uniquewhatever.xml
    if (filename.toString().toUpperCase().endsWith(".XML") ) {
        filenamesplit = filename.split("\\.", -1);
        if (filenamesplit.length >= 3 ) {
               senderid = filenamesplit[0].toString().toUpperCase();  // tpid will be first element in filename
        } 
    }
   
            control[0] = senderid;
            control[1] = "XML";
            control[2] = "";
            control[3] = filename;
            control[4] = "-1";
            control[5] = "-1";
            control[6] = "-1";
            control[7] = "";
    if (senderid.isEmpty()) {
        OVData.writeEDILog(control, "0", "ERROR", "XML file with no senderid " + filename);
        proceed = false;
    }
    
    if (proceed) {
        
        
        // now lets get map
             map = OVData.getEDIInMap(senderid, doctype);
               if (! map.isEmpty()) {
                   control[2] = map;
                    try {
                    Class cls = Class.forName("EDIMaps." + map);
                    Object obj = cls.newInstance();
                    Method method = cls.getDeclaredMethod("MapXMLdata", File.class, String.class, String.class);
                    method.invoke(obj, file, control, senderid);
                    
                    } catch (IllegalAccessException | ClassNotFoundException |
                             InstantiationException | NoSuchMethodException |
                            InvocationTargetException ex) {
                        OVData.writeEDILog(control, "0", "ERROR", "unable to find map class for " + senderid + " / " + doctype);
                        ex.printStackTrace();
                    }
               }   else {
                   OVData.writeEDILog(control, "0", "ERROR", "no edi_mstr map for " + senderid + " / " + doctype);
                   return;
               }
             }
    
    
    
    }
    
    public static void processCSV(char[] cbuf, String filename)   {
    ArrayList<String> doc = new ArrayList<String>();
    String[] filenamesplit = null;
        String doctype = "CSV";
        String controlnum = "-1";
        String docid = "-1";
        String map = "";
        String senderid = "";
        String[] control= initEDIControl();
        boolean proceed = true;
    //lets first get the tpid from the filename convention.....
    // filename convention must be tpid.uniquewhatever.csv or tpid.uniquewhatever.xml
    if (filename.toString().toUpperCase().endsWith(".CSV") || filename.toString().toUpperCase().endsWith(".XML") ) {
        filenamesplit = filename.split("\\.", -1);
        if (filenamesplit.length >= 3 ) {
               senderid = filenamesplit[0].toString().toUpperCase();  // tpid will be first element in filename
        } 
    }
            control[0] = senderid;
            control[1] = "CSV";
            control[2] = "";
            control[3] = filename;
            control[4] = "-1";
            control[5] = "-1";
            control[6] = "-1";
            control[7] = "";
    
    if (senderid.isEmpty()) {
        OVData.writeEDILog(control, "0", "ERROR", "CSV file with no senderid " + filename);
        proceed = false;
    }
    
    if (proceed) {
        
        // build array of character buffers...elements separated by newline
         StringBuilder segment = new StringBuilder();
    for (int i = 0; i < cbuf.length; i++) {
        if (cbuf[i] == '\n') {
            doc.add(segment.toString());
            segment.delete(0, segment.length());
        } else {
        segment.append(cbuf[i]);
        }
    }
        
        // now lets get map
             map = OVData.getEDIInMap(senderid, doctype);
               if (! map.isEmpty()) {
                    try {
                    Class cls = Class.forName("EDIMaps." + map);
                    Object obj = cls.newInstance();
                    Method method = cls.getDeclaredMethod("MapCSVdata", ArrayList.class, String.class, String.class);
                    method.invoke(obj, doc, control, senderid);
                   // OVData.writeEDILog(control, "INFO", "processing inbound file");
                    } catch (IllegalAccessException | ClassNotFoundException |
                             InstantiationException | NoSuchMethodException |
                            InvocationTargetException ex) {
                        OVData.writeEDILog(control, "0", "ERROR", "unable to find map class for " + senderid + " / " + doctype);
                        ex.printStackTrace();
                    }
               }   else {
                   OVData.writeEDILog(control, "0", "ERROR", "no edi_mstr map for " + senderid + " / " + doctype);
                   return;
               }
             }
    
    
    
    }
    
    public static void processEDIFACT(char[] cbuf, String filename)   {
    ArrayList<String> doc = new ArrayList<String>();
    
    char flddelim = '+';
    char subdelim = ':';
    char segdelim = '\'';
    
    String flddelim_str = String.valueOf(flddelim);
    String subdelim_str = String.valueOf(subdelim);
    String segdelim_str = String.valueOf(segdelim);
    
     if (flddelim_str.equals("+")) {
            flddelim_str = "\\+";
        }
    
    StringBuilder segment = new StringBuilder();
    for (int i = 0; i < cbuf.length; i++) {
        if (cbuf[i] == segdelim) {
            doc.add(segment.toString());
            segment.delete(0, segment.length());
        } else {
        segment.append(cbuf[i]);
        }
    }
        
    /* let's get the ISA and GS info and determine the class map to call based on sender ID */
        int i = 0;
        int start = 0;
        int stop = 0;
        String senderid = "";
        String doctype = "";
        String docid = "";
        String map = "";
        String ISASTRING = "";
        String controlnum = "";
        String GSSTRING = "";
        String[] control= initEDIControl();
        boolean proceed = false;
        for (Object seg : doc) {
             String[] segarr = seg.toString().split(flddelim_str);
           if (segarr[0].toString().equals("UNB")) {
             senderid = segarr[2].split(subdelim_str,-1)[0];
             controlnum = segarr[5];
             ISASTRING = (String)seg;
           }
          
           if (segarr[0].toString().equals("UNH")) {
             proceed = true;
             start = i;
             if (segarr.length > 3 && segarr[2].split(subdelim_str,-1)[0].equals("DELJIT") ) {
             doctype = segarr[2].split(subdelim_str,-1)[0] + segarr[3];  // combine deljit with SH or AS qualifier (862=sh or 850=as).
             } else if ( segarr.length <= 3 && segarr[2].split(subdelim_str,-1)[0].equals("DELJIT")  ) {
             doctype = segarr[2].split(subdelim_str,-1)[0] + "SH";    // if no 4th element and Deljit...assume 862=sh
             } else {
             doctype = segarr[2].split(subdelim_str,-1)[0];    // any other edifact document
             }
             docid = segarr[1];
             // set control
            control[0] = senderid;
            control[1] = doctype;
            control[2] = "Unknown";
            control[3] = filename;
            control[4] = controlnum;
            control[5] = "Unknown";
            control[6] = "Unknown";
            control[7] = "Unknown";
             
             
              // skip if this is an acknowledgement and/or rip n read type doc
             if ( doctype.equals("APERAK") ) {
             OVData.writeEDILog(control, "0", "INFO", "read only");
             proceed = false;
             }
             
             
           }
           if (segarr[0].toString().equals("UNT")) {
             stop = i;
             ArrayList<String> thisdoc = new ArrayList<String>(doc.subList(start, stop + 1));
             thisdoc.add(0, GSSTRING);  // add the GS segment back on top
             thisdoc.add(0, ISASTRING);  // add the ISA segment back on top
             
            
             
             if (proceed) {
             map = OVData.getEDIInMap(senderid, doctype);
               if (! map.isEmpty()) {
                   control[2] = map;
                    try {
                    Class cls = Class.forName("EDIMaps." + map);
                    Object obj = cls.newInstance();
                    Method method = cls.getDeclaredMethod("Mapdata", ArrayList.class, String.class, String.class, String.class);
                    method.invoke(obj, thisdoc, flddelim_str, subdelim_str, control);
                    OVData.writeEDILog(control, "0", "INFO", "processing inbound file");
                    } catch (IllegalAccessException | ClassNotFoundException |
                             InstantiationException | NoSuchMethodException |
                            InvocationTargetException ex) {
                        OVData.writeEDILog(control, "0", "ERROR", "unable to find map class for " + senderid + " / " + doctype);
                        ex.printStackTrace();
                    }
               }   else {
                   OVData.writeEDILog(control, "0", "ERROR", "no edi_mstr map for " + senderid + " / " + doctype);
                   return;
               }
             }
             
           }
           i++;
         }
      
}
    
    public static void processX12(Map<Integer, Object[]> ISAmap, char[] cbuf, String batchfile)   {
    
         /*  22 elements consisting of:
            c[0] = senderid;
            c[1] = doctype;
            c[2] = map;
            c[3] = infile;
            c[4] = controlnum;
            c[5] = gsctrlnbr;
            c[6] = docid;
            c[7] = ref;
            c[8] = outfile;
            c[9] = sd;
            c[10] = ed;
            c[11] = ud;
            c[12] = overideenvelope
            c[13] = isastring
            c[14] = gsstring
            c[15] = dir
            c[16] = idxnbr
            c[17] = isastart
            c[18] = isaend
            c[19] = docstart
            c[20] = docend
            c[21] = receiverid;
              */
    
    // ISAmap is defined as new Integer[] {start, end, (int) s, (int) e, (int) u});
    
    // loop through ISAMap entries
    
             
             
    for (Map.Entry<Integer, Object[]> isa : ISAmap.entrySet()) {
        int start =  Integer.valueOf(isa.getValue()[0].toString());  //starting ISA position in file
        int end = Integer.valueOf(isa.getValue()[1].toString());  // ending IEA position in file
        char segdelim = (char) Integer.valueOf(isa.getValue()[2].toString()).intValue(); // get segment delimiter
        
       //  System.out.println("envelope key/start/end : " + isa.getKey() + "/" + isa.getValue()[0] + "/" + isa.getValue()[1]);
        
        String[] c = (String[]) isa.getValue()[6];
       // ArrayList d = (ArrayList) isa.getValue()[5];
        Map<Integer, ArrayList> d = (HashMap<Integer, ArrayList>)isa.getValue()[5];
        
     //   System.out.println("doc entryset is : " + d.entrySet());
        
        ArrayList<String> falist = new ArrayList<String>();
        
       //for (Object z : d) {
       for (Map.Entry<Integer, ArrayList> z : d.entrySet()) {
         
       //    System.out.println("doc May entry key: " + z.getKey());
           
         for (Object r : z.getValue()) {
            Object[] x = (Object[]) r;
         
            Integer[] k = (Integer[])x[0];
            String doctype = (String)x[1];
            String docid = (String)x[2];
           
            c[1] = doctype;
            c[6] = docid;
      
       //    System.out.println("doc key/{start/end},doctype,docid " + k[0] + "/" + k[1] + "/" + doctype + "/" + docid);
            
           // Integer[] k = (Integer[])z.getValue()[0];
           // String doctype = (String)z.getValue()[1];
           // String docid = (String)z.getValue()[2];
           
            
         //   System.out.println("doc start/end : " + k[0] + "/" + k[1]);
        falist.add(docid); // add ST doc id to falist for functional acknowledgement
      //        System.out.println("control values: " + docid + "/" + k[0] + "/" + k[1] );
       
    // here you are inserting 'segments' into ArrayList doc
    StringBuilder segment = new StringBuilder();
   // for (int i = 0; i < cbuf.length; i++) {
   ArrayList<String> doc = new ArrayList<String>();
   for (int i = k[0]; i < k[1]; i++) {
        if (cbuf[i] == segdelim) {
            doc.add(segment.toString());
            segment.delete(0, segment.length());
        } else {
            if (! (String.format("%02x",(int) cbuf[i]).equals("0d") || String.format("%02x",(int) cbuf[i]).equals("0a")) ) {
                segment.append(cbuf[i]);
            } 
        }
    }
     
             c[3] = batchfile;
             
             
        // insert isa and st start and stop integer points within the file
        
          c[17] = String.valueOf(start);
          c[18] = String.valueOf(end);
          c[19] = String.valueOf(k[0]);
          c[20] = String.valueOf(k[1]);
          
          // at this point...we need to log this doc in edi_idx table and use return ID for further logs against this doc idx.
          if (c[12].isEmpty()) {   // if not override
          int idxnbr = OVData.writeEDIIDX(c);
          c[16] = String.valueOf(idxnbr);
          }
             String map = c[2];
             
               if (map.isEmpty() && c[12].isEmpty()) {
                  map = OVData.getEDIInMap(c[0], c[1]); 
               } 
            
               // if no map then bail
               if (map.isEmpty() && c[12].isEmpty()) {
                  OVData.writeEDILog(c, "0", "ERROR", "unable to find map class for " + c[0] + " / " + c[1]); 
               } else {
                   
                   // at this point I should have a doc set (ST to SE) and a map ...now call map to operate on doc 
                    try {
                    Class cls = Class.forName(map);
                    Object obj = cls.newInstance();
                    Method method = cls.getDeclaredMethod("Mapdata", ArrayList.class, String[].class);
                    method.invoke(obj, doc, c);
                  
                    } catch (IllegalAccessException | ClassNotFoundException |
                             InstantiationException | NoSuchMethodException |
                            InvocationTargetException ex) {
                        if (c[12].isEmpty()) {
                        OVData.writeEDILog(c, "0", "ERROR", "unable to load map class for " + c[0] + " / " + c[1]);
                        }
                        ex.printStackTrace();
                    }
                   
               }
               
            
               
      } // r         
               
    } // object k
        
             
       
            // 997
            
            ArrayList<String> falistcopy = new ArrayList<String>(falist);
            falist.clear();
            
                if (c[12].isEmpty() && BlueSeerUtils.ConvertStringToBool(OVData.getEDIFuncAck(c[0], c[1]))) {
                    try {
                    String[] _isa = c[13].toString().split(EDI.escapeDelimiter(ed), -1);
                    String[] _gs = c[14].toString().split(EDI.escapeDelimiter(ed), -1);
                    Class cls = Class.forName("EDIMaps." + "Generic997o");
                    Object obj = cls.newInstance();
                    Method method = cls.getDeclaredMethod("Mapdata", ArrayList.class, String[].class);
                    method.invoke(obj, falistcopy, c);
                   // OVData.writeEDILog(control, "INFO", "processing inbound file");
                    } catch (IllegalAccessException | ClassNotFoundException |
                             InstantiationException | NoSuchMethodException |
                            InvocationTargetException ex) {
                        OVData.writeEDILog(c, "0", "ERROR", "Problem generating 997 for " + c[0] + " / " + c[1]);
                        ex.printStackTrace();
                    }
                }
            
     
     
    } // ISAMap entries
   
  
}
    

    public static ArrayList<String> getEnvFromFileAsArrayListwRegex(File file) throws FileNotFoundException, IOException, ClassNotFoundException {
        
        ArrayList<String> envelopes = new ArrayList<String>();
        
         StringBuilder contents = new StringBuilder();
         BufferedReader f = new BufferedReader(new FileReader(file));
         String text = "";
         while ((text = f.readLine()) != null) {
         contents.append(text).append("&");
         //contents.append(text);
         }
         f.close();
       Pattern p = Pattern.compile("(ISA(?:(?!ISA.*GS.*GE.*IEA).).*?GS.*?GE.*?IEA)");
       Matcher m = p.matcher(contents);
       int i = 0;
       
       while(m.find()) {
            envelopes.add(m.group(i));
            i++;
        }
      return envelopes;
    }
     
    public static ArrayList<String[]> getSegFromEnvAsArrayList(ArrayList<String> envelopes)  {
        String[] segments = null;
        ArrayList<String[]> mylist = new ArrayList<String[]>();  
         String sd = "";
         String ed = "";
         String ud = "";
         
         for (String s : envelopes) {
          // one 's' record per envelope....s = envelope ISA to IEA
          ed = s.substring(103,104);
          ud = s.substring(104,105);
          sd = s.substring(105,106);
          /*
          if (sd.toCharArray().length > 1) {
          if (String.format("%02x", (int) sd.toCharArray()[0]).equals("0d")) {
              
          }
          }
          */
          
          System.out.println(s);
          System.out.println(sd);
          
        
          
          if (sd.equals("\\")) {
            sd = "\\\\";
          }
          
       
          segments  = s.split(sd);
          mylist.add(segments);
         }
         
      return mylist;
    }
    
    
    // inbound
    public static void createOrderFrom830(edi830 e, String[] control) {
        
        int sonbr = 0;
        
        String orderline = "";
        String[] orderlinearray  = null;
        String[] fcsarray = null;
        String order = "";
        String line = "";
        
            control[7] = e.ov_shipto;
           OVData.writeEDILog(control, "0", "INFO", "Load");
            // control = e.isaSenderID + "," + e.doctype + "," + e.isaCtrlNum + "," + e.po ;
             
             
             
             //OVData.CreateSalesOrderHdr(control, String.valueOf(sonbr), e.ov_billto, shipto, e.po, e.duedate, e.podate, e.remarks);
               for (int j = 0; j < e.getDetCount(); j++ ) {
                  
               // find the blanket order type with key shipto, part, po ...if not availabe raise error and next loop    
               orderline = OVData.GetBlanketOrderLine(control, e.ov_shipto, e.getDetItem(j), e.getDetPO(j));
               
               
                   if (! orderline.isEmpty()) {
                       orderlinearray = orderline.split(",", -1);
                       order = orderlinearray[0].toString();
                       line = orderlinearray[1].toString();

                     
                       for (String mystring : e.map.get(j)  ) { 
                           fcsarray = mystring.split(",", -1);
                           OVData.CreateSalesOrderSchedDet(
                                   order,    // sales order
                                   line,    // order line
                                   fcsarray[0],
                                   fcsarray[1],
                                   fcsarray[2],
                                   fcsarray[3],
                                   e.rlse
                                   );
                                 
                       }
                             
                   } // if orderline not empty
               
               } // for each j
       
       
    
    }
    
    public static void createOrderFrom862(edi862 e, String[] control) {
        
        int sonbr = 0;
       
        String orderline = "";
        String[] orderlinearray  = null;
        String[] fcsarray = null;
        String order = "";
        String line = "";
        
         control[7] = e.ov_shipto;
        OVData.writeEDILog(control, "0", "INFO", "Load");
        //   control = e.isaSenderID + "," + e.doctype + "," + e.isaCtrlNum + "," + e.po ;
             
             
             
             //OVData.CreateSalesOrderHdr(control, String.valueOf(sonbr), e.ov_billto, shipto, e.po, e.duedate, e.podate, e.remarks);
               for (int j = 0; j < e.getDetCount(); j++ ) {
                  
               // find the blanket order type with key shipto, part, po ...if not availabe raise error and next loop    
               orderline = OVData.GetBlanketOrderLine(control, e.ov_shipto, e.getDetItem(j), e.getDetPO(j));
               
               
                   if (! orderline.isEmpty()) {
                       orderlinearray = orderline.split(",");
                       order = orderlinearray[0].toString();
                       line = orderlinearray[1].toString();

                     
                       for (String mystring : e.map.get(j)  ) { 
                         
                           fcsarray = mystring.split(",");
                           OVData.CreateSalesOrderSchedDet(
                                   order,    // sales order
                                   line,    // order line
                                   fcsarray[0],
                                   fcsarray[1],
                                   fcsarray[2],
                                   fcsarray[3],
                                   e.rlse
                                   );
                                 
                       }
                             
                   } // if orderline not empty
               
               } // for each j
       
       
   
    }
    
    public static void createOrderFrom850(edi850 e, String[] control) {
        
        int sonbr = 0;
        boolean error = false;
        
        String shipto = "";
        control[7] = String.valueOf(sonbr);
       
            error = false;
             sonbr = OVData.getNextNbr("order");
             OVData.writeEDILog(control, "0", "INFO", "Load");
           //  control = ((edi850)e.get(i)).isaSenderID + "," + ((edi850)e.get(i)).doctype + "," + ((edi850)e.get(i)).isaCtrlNum + "," + ((edi850)e.get(i)).po ;
             
             if (e.ov_shipto.isEmpty())
                 shipto = OVData.CreateShipTo(e.ov_billto, e.st_name, e.st_line1, e.st_line2, e.st_line3, e.st_city, e.st_state, e.st_zip, e.st_country, e.shipto);
             else
                 shipto = e.ov_shipto;
             
             error = OVData.CreateSalesOrderHdr(control, String.valueOf(sonbr), e.ov_billto, shipto, e.po, e.duedate, e.podate, e.remarks, e.shipmethod); 
             if (! error) {  
             for (int j = 0; j < e.getDetCount(); j++ ) {
                 
                   // error trigger logic ...missing internal item
                   if (e.getDetItem(j).isEmpty())
                      error = true;
                   
                   // error trigger logic ...missing netprice = 0
                   if ( Double.valueOf(e.getDetNetPrice(j)) == 0)
                      error = true;
                  
               OVData.CreateSalesOrderDet(String.valueOf(sonbr), 
                       e.ov_billto,
                       e.getDetItem(j), 
                       e.getDetCustItem(j), 
                       e.getDetSku(j), 
                       e.getDetPO(j), 
                       e.getDetQty(j),
                       e.getDetListPrice(j), 
                       e.getDetDisc(j), 
                       e.getDetNetPrice(j),
                       e.getHdrDueDate(), 
                       e.getDetRef(j),
                       String.valueOf(j + 1)); 
               // System.out.println(((edi850)e.get(i)).getDetCustItem(j));
               }
             }
        if (error)
            OVData.updateOrderStatusError(String.valueOf(sonbr));
        
    
    }
    
    public static void createShipperFrom945(edi945 e, String[] control) {
        
        int shipperid = 0;
        boolean error = false;
        
        String shipto = "";
       
            error = false;
             shipperid = OVData.getNextNbr("shipper");
             control[7] = String.valueOf(shipperid);
             OVData.writeEDILog(control, "0", "INFO", "Load");
           //  control = ((edi850)e.get(i)).isaSenderID + "," + ((edi850)e.get(i)).doctype + "," + ((edi850)e.get(i)).isaCtrlNum + "," + ((edi850)e.get(i)).po ;
             
             if (e.ov_shipto.isEmpty())
                 shipto = OVData.CreateShipTo(e.ov_billto, e.st_name, e.st_line1, e.st_line2, e.st_line3, e.st_city, e.st_state, e.st_zip, e.st_country, e.shipto);
             else
                 shipto = e.ov_shipto; 
             
             error = OVData.CreateShipperHdrEDI(control, String.valueOf(shipperid), e.shipper, e.ov_billto, shipto, e.so, e.po, e.shipdate, e.podate, e.remarks, e.shipmethod);     
             if (! error) {  
             for (int j = 0; j < e.getDetCount(); j++ ) {
                 
                   // error trigger logic ...missing internal item
                   if (e.getDetItem(j).isEmpty())
                      error = true;
                   
                   // error trigger logic ...missing netprice = 0
                   if ( Double.valueOf(e.getDetNetPrice(j)) == 0)
                      error = true;
                  
               OVData.CreateShipperDet(String.valueOf(shipperid),
                       e.getDetItem(j), 
                       e.getDetCustItem(j), 
                       e.getDetSku(j),
                       e.so,
                       e.getDetPO(j), 
                       e.getDetQtyShp(j),
                       e.getDetUOM(j),
                       e.getDetListPrice(j), 
                       e.getDetDisc(j), 
                       e.getDetNetPrice(j),
                       e.getShipDate(), 
                       e.getDetDesc(j),
                       e.getDetLine(j),
                       e.getDetSite(j),
                       e.getDetWH(j),
                       e.getDetLoc(j),
                       "0"   // this is a holder for matltax which is not implemented in EDI yet
               ); 
               // System.out.println(((edi850)e.get(i)).getDetCustItem(j));
               }
             }
       
    
    }
      
    public static void createFOTDETFrom990(edi990 e, String[] control) {
             control[7] = e.order;
             OVData.writeEDILog(control, "0", "INFO", "Load");
             OVData.CreateFOTDETFrom990i(control, e.order, e.scac, e.yesno, e.reasoncode);  
    
    }
     
    public static void createFOTDETFrom220(edi220 e, String[] control) {
             control[7] = e.order;
             OVData.writeEDILog(control, "0", "INFO", "Load");
             OVData.CreateFOTDETFrom220i(control, e.order, e.scac, e.yesno, e.remarks, e.amount);   
    
    }
    
    public static void createFOMSTRFrom204i(edi204i e, String[] control) {
             control[7] = e.custfo;
             String fo = OVData.CreateFOMSTRFrom204i(control, e.custfo, e.carrier, e.equiptype, e.remarks, e.bol, e.cust, e.tpid, e.weight, e.ref);  
             for (int j = 0; j < e.getDetCount(); j++ ) {
               OVData.CreateFODDETFrom204i(fo,
                       e.getDetLine(j), 
                       e.getDetType(j), 
                       e.getDetShipper(j),
                       e.getDetDelvDate(j),
                       e.getDetDelvTime(j),
                       e.getDetShipDate(j),
                       e.getDetShipTime(j),
                       e.getDetAddrCode(j),
                       e.getDetAddrName(j), 
                       e.getDetAddrLine1(j), 
                       e.getDetAddrLine2(j),
                       e.getDetAddrCity(j), 
                       e.getDetAddrState(j),
                       e.getDetAddrZip(j),
                       e.getDetUnits(j),
                       e.getDetBoxes(j),
                       e.getDetWeight(j),
                       e.getDetWeightUOM(j),
                       e.getDetRef(j),
                       e.getDetRemarks(j)); 
               // System.out.println(((edi850)e.get(i)).getDetCustItem(j));
               }
             
             
    
    } 
     
    public static void createFOTDETFrom214(edi214 e, String[] control) {
             control[7] = e.order;
             OVData.writeEDILog(control, "0", "INFO", "Load");
             OVData.CreateFOTDETFrom214i(control, e.order, e.scac, e.pronbr, e.status, e.remarks, e.lat, e.lon, e.equipmentnbr, e.equipmenttype, e.apptdate, e.appttime);   
    
    }
      
    public static void createOrderFromCSV(ArrayList e, String[] control) {
        
        int sonbr = 0;
        boolean error = false;
        String shipto = "";
        
       
        for (int i = 0; i < e.size(); i++) {
            error = false;
             sonbr = OVData.getNextNbr("order");
             control[7] = ((edi850) e.get(i)).po;
             OVData.writeEDILog(control, "0", "INFO", "Load");
           //  control = ((edi850)e.get(i)).isaSenderID + "," + ((edi850)e.get(i)).doctype + "," + ((edi850)e.get(i)).isaCtrlNum + "," + ((edi850)e.get(i)).po ;
             
             if (((edi850) e.get(i)).ov_shipto.isEmpty())
                 shipto = OVData.CreateShipTo(((edi850) e.get(i)).ov_billto, ((edi850) e.get(i)).st_name, ((edi850) e.get(i)).st_line1, ((edi850) e.get(i)).st_line2, ((edi850) e.get(i)).st_line3, ((edi850) e.get(i)).st_city, ((edi850) e.get(i)).st_state, ((edi850) e.get(i)).st_zip, ((edi850) e.get(i)).st_country, ((edi850) e.get(i)).shipto);
             else
                 shipto = ((edi850) e.get(i)).ov_shipto;
             
             OVData.CreateSalesOrderHdr(control, String.valueOf(sonbr), ((edi850) e.get(i)).ov_billto, shipto, ((edi850) e.get(i)).po, ((edi850) e.get(i)).duedate, ((edi850) e.get(i)).podate, ((edi850) e.get(i)).remarks, ((edi850) e.get(i)).shipmethod);
               for (int j = 0; j < ((edi850) e.get(i)).getDetCount(); j++ ) {
                 
                   // error trigger logic ...missing internal item
                   if (((edi850) e.get(i)).getDetItem(j).isEmpty())
                      error = true;
                   
                   // error trigger logic ...missing netprice = 0
                   if ( Double.valueOf(((edi850) e.get(i)).getDetNetPrice(j)) == 0)
                      error = true;
                  
               OVData.CreateSalesOrderDet(String.valueOf(sonbr), 
                       ((edi850) e.get(i)).ov_billto,
                       ((edi850) e.get(i)).getDetItem(j), 
                       ((edi850) e.get(i)).getDetCustItem(j), 
                       ((edi850) e.get(i)).getDetSku(j), 
                       ((edi850) e.get(i)).getDetPO(j), 
                       ((edi850) e.get(i)).getDetQty(j),
                       ((edi850) e.get(i)).getDetListPrice(j), 
                       ((edi850) e.get(i)).getDetDisc(j), 
                       ((edi850) e.get(i)).getDetNetPrice(j),
                       ((edi850) e.get(i)).duedate, 
                       ((edi850) e.get(i)).getDetRef(j),
                       String.valueOf(j + 1)); 
               // System.out.println(((edi850)e.get(i)).getDetCustItem(j));
               }
       
        if (error)
            OVData.updateOrderStatusError(String.valueOf(sonbr));
        }
    
    }
     
    public static void createOrderFromXML(ArrayList e, String[] control) {
        
        int sonbr = 0;
        boolean error = false;
        String shipto = "";
       
        for (int i = 0; i < e.size(); i++) {
            error = false;
             sonbr = OVData.getNextNbr("order");
             control[7] = ((edi850) e.get(i)).po;
             OVData.writeEDILog(control, "0", "INFO", "Load");
           //  control = ((edi850)e.get(i)).isaSenderID + "," + ((edi850)e.get(i)).doctype + "," + ((edi850)e.get(i)).isaCtrlNum + "," + ((edi850)e.get(i)).po ;
             
             if (((edi850) e.get(i)).ov_shipto.isEmpty())
                 shipto = OVData.CreateShipTo(((edi850) e.get(i)).ov_billto, ((edi850) e.get(i)).st_name, ((edi850) e.get(i)).st_line1, ((edi850) e.get(i)).st_line2, ((edi850) e.get(i)).st_line3, ((edi850) e.get(i)).st_city, ((edi850) e.get(i)).st_state, ((edi850) e.get(i)).st_zip, ((edi850) e.get(i)).st_country, ((edi850) e.get(i)).shipto);
             else
                 shipto = ((edi850) e.get(i)).ov_shipto;
             
             OVData.CreateSalesOrderHdr(control, String.valueOf(sonbr), ((edi850) e.get(i)).ov_billto, shipto, ((edi850) e.get(i)).po, ((edi850) e.get(i)).duedate, ((edi850) e.get(i)).podate, ((edi850) e.get(i)).remarks, ((edi850) e.get(i)).shipmethod);
               for (int j = 0; j < ((edi850) e.get(i)).getDetCount(); j++ ) {
                 
                   // error trigger logic ...missing internal item
                   if (((edi850) e.get(i)).getDetItem(j).isEmpty())
                      error = true;
                   
                   // error trigger logic ...missing netprice = 0
                   if ( Double.valueOf(((edi850) e.get(i)).getDetNetPrice(j)) == 0)
                      error = true;
                  
               OVData.CreateSalesOrderDet(String.valueOf(sonbr), 
                       ((edi850) e.get(i)).ov_billto,
                       ((edi850) e.get(i)).getDetItem(j), 
                       ((edi850) e.get(i)).getDetCustItem(j), 
                       ((edi850) e.get(i)).getDetSku(j), 
                       ((edi850) e.get(i)).getDetPO(j), 
                       ((edi850) e.get(i)).getDetQty(j),
                       ((edi850) e.get(i)).getDetListPrice(j), 
                       ((edi850) e.get(i)).getDetDisc(j), 
                       ((edi850) e.get(i)).getDetNetPrice(j),
                       ((edi850) e.get(i)).duedate, 
                       ((edi850) e.get(i)).getDetRef(j),
                       String.valueOf(j + 1)); 
               // System.out.println(((edi850)e.get(i)).getDetCustItem(j));
               }
       
        if (error)
            OVData.updateOrderStatusError(String.valueOf(sonbr));
        }
    
    }
     
      
    // outbound
    public static int Create856(String shipper)  {
          int errorcode = 0;
        // errorcode = 0 ... clean exit
        // errorcode = 1 ... no record found in cmedi_mstr for billto doctype
        // errorcode = 2 ... unable to retrieve billto from shipper
        // errorcode = 3 ... any catch error below ...try running from command line to see trace dump
          // errorcode = 4 ... shipper does not exist
          
        String billto = "";
        String doctype = "856";
        String map = "";
        boolean proceed = true;
       
         
        // lets determine the billto of this shipper
        billto = OVData.getShipperBillto(shipper);
        if (billto.isEmpty()) {
            proceed = false;
            errorcode = 2;
            return errorcode;
        }
        
        String[] c_in = initEDIControl();   
        
        c_in[0] = billto;
        c_in[1] = doctype;
        c_in[2] = map;
        c_in[3] = "";
        c_in[4] = "";
        c_in[5] = "";
        c_in[6] = "";
        c_in[7] = shipper;
        c_in[15] = "0"; // dir out
        c_in[12] = "0"; // is override
        
        // get Delimiters from Cust Defaults
        String[] defaults = OVData.getEDIOutCustDefaults(billto, doctype, "0");
        c_in[9] = defaults[7]; 
        c_in[10] = defaults[6]; 
        c_in[11] = defaults[8]; 
        
        // lets determine if an ASN map is available for this billto of this shipper
        map = OVData.getEDIOutMap(c_in[0], c_in[1]);
        
          if (map.isEmpty()) {
            proceed = false;
            errorcode = 1;
            OVData.writeEDILog(c_in, "1", "ERROR", "no edi_mstr map for " + billto + " / " + doctype); 
                   return errorcode;
        } 
        
        
        // Mapdata method call below requires two parameters (ArrayList, String[]) ...doc and c
        ArrayList doc = new ArrayList();
        doc.add(shipper);
        
        
        
           if (proceed) {
                   try {
                    Class cls = Class.forName(map);
                    Object obj = cls.newInstance();
                    Method method = cls.getDeclaredMethod("Mapdata", ArrayList.class, String[].class);
               
                    
                    Object envelope = method.invoke(obj, doc, c_in); // envelope array holds in this order (isa, gs, ge, iea, filename, isactrlnum, gsctrlnum, stctrlnum)
                    String[] c_out = (String[])envelope;
                    
                    OVData.writeEDILog(c_out, "1", "INFO", "Export"); 
                    } catch (IllegalAccessException | ClassNotFoundException |
                             InstantiationException | NoSuchMethodException |
                            InvocationTargetException ex) {
                         if (c_in[12].isEmpty()) {
                        OVData.writeEDILog(c_in, "0", "ERROR", "unable to load map class for " + c_in[0] + " / " + c_in[1]);
                        }
                        errorcode = 3;
                        ex.printStackTrace();
                    }
                  
           }
         
         
         return errorcode;
        
         
     }
     
    public static int Create810(String shipper)  {
        int errorcode = 0;
        // errorcode = 0 ... clean exit
        // errorcode = 1 ... no record found in cmedi_mstr for billto doctype
        // errorcode = 2 ... unable to retrieve billto from shipper
        // errorcode = 3 ... any catch error below ...try running from command line to see trace dump
        // errorcode = 4 ... shipper does not exist
        
        
        String billto = "";
        String doctype = "810";
        String map = "";
        boolean proceed = true;
       
         
        // lets determine the billto of this shipper
        billto = OVData.getShipperBillto(shipper);
        if (billto.isEmpty()) {
            proceed = false;
            errorcode = 2;
            return errorcode;
        }
        
        String[] c_in = initEDIControl();   
        
        c_in[0] = billto;
        c_in[1] = doctype;
        c_in[2] = map;
        c_in[3] = "";
        c_in[4] = "";
        c_in[5] = "";
        c_in[6] = "";
        c_in[7] = shipper;
        c_in[15] = "0"; // dir out
        c_in[12] = "0"; // is override
        
        // get Delimiters from Cust Defaults
        String[] defaults = OVData.getEDIOutCustDefaults(billto, doctype, "0");
        c_in[9] = defaults[7]; 
        c_in[10] = defaults[6]; 
        c_in[11] = defaults[8]; 
        
        // lets determine if an ASN map is available for this billto of this shipper
        map = OVData.getEDIOutMap(c_in[0], c_in[1]);
        
          if (map.isEmpty()) {
            proceed = false;
            errorcode = 1;
            OVData.writeEDILog(c_in, "1", "ERROR", "no edi_mstr map for " + billto + " / " + doctype); 
                   return errorcode;
        } 
        
        
        // Mapdata method call below requires two parameters (ArrayList, String[]) ...doc and c
        ArrayList doc = new ArrayList();
        doc.add(shipper);
        
        
           if (proceed) {
                    try {
                    Class cls = Class.forName(map);
                    Object obj = cls.newInstance();
                    Method method = cls.getDeclaredMethod("Mapdata", ArrayList.class, String[].class);
               
                    
                    Object envelope = method.invoke(obj, doc, c_in); // envelope array holds in this order (isa, gs, ge, iea, filename, isactrlnum, gsctrlnum, stctrlnum)
                    String[] c_out = (String[])envelope;
                    
                    OVData.writeEDILog(c_out, "1", "INFO", "Export"); 
                    } catch (IllegalAccessException | ClassNotFoundException |
                             InstantiationException | NoSuchMethodException |
                            InvocationTargetException ex) {
                         if (c_in[12].isEmpty()) {
                        OVData.writeEDILog(c_in, "0", "ERROR", "unable to load map class for " + c_in[0] + " / " + c_in[1]);
                        }
                        errorcode = 3;
                        ex.printStackTrace();
                    }
                  
           }
         
         
         
       return errorcode; 
         
     }
     
    public static int Create940(String nbr)  {
        int errorcode = 0;
       
        // errorcode = 0 ... clean exit
        // errorcode = 1 ... no record found in wh_mstr for wh doctype
        // errorcode = 2 ... unable to retrieve wh from order
        // errorcode = 3 ... any catch error below ...try running from command line to see trace dump
        // errorcode = 4 ... shipper does not exist
        
        
        ArrayList<String> wh = new ArrayList();
        String doctype = "940";
        String map = "";
        String[] control = null;
        
        // lets determine the warehouse or warehouses for this order
        wh = OVData.getOrderWHSource(nbr);
        if (wh.size() == 0) {
            return 2;
        }
        
         
        for (String w : wh) {   
        
        map = OVData.getEDIOutMap(w, doctype);
        
         String[] c_in = initEDIControl();   // controlarray in this order : entity, doctype, map, filename, isacontrolnum, gsctrlnum, stctrlnum, ref ; 
       
        c_in[0] = w;
        c_in[1] = doctype;
        c_in[2] = map;
        c_in[3] = "";
        c_in[4] = "";
        c_in[5] = "";
        c_in[6] = "";
        c_in[7] = nbr;
        c_in[15] = "0"; // dir out
        c_in[12] = "0"; // is override
        
        // get Delimiters from Cust Defaults
        String[] defaults = OVData.getEDIOutCustDefaults(w, doctype, "0");
        c_in[9] = defaults[7]; 
        c_in[10] = defaults[6]; 
        c_in[11] = defaults[8]; 
        
        
        if (map.isEmpty()) {
            OVData.writeEDILog(c_in, "1", "ERROR", "no edi_mstr map for " + w + " / " + doctype);
            errorcode = 1;
                   continue;
        } 
        
         // Mapdata method call below requires two parameters (ArrayList, String[]) ...doc and c
        ArrayList doc = new ArrayList();
        doc.add(nbr);
                   
                      try {
                    Class cls = Class.forName(map);
                    Object obj = cls.newInstance();
                    Method method = cls.getDeclaredMethod("Mapdata", ArrayList.class, String[].class);
               
                    
                    Object envelope = method.invoke(obj, doc, c_in); // envelope array holds in this order (isa, gs, ge, iea, filename, isactrlnum, gsctrlnum, stctrlnum)
                    String[] c_out = (String[])envelope;
                    
                    OVData.writeEDILog(c_out, "1", "INFO", "Export"); 
                    OVData.CreateFreightEDIRecs(c_in, nbr);
                    

                    } catch (IllegalAccessException | ClassNotFoundException |
                             InstantiationException | NoSuchMethodException |
                            InvocationTargetException ex) {
                        OVData.writeEDILog(c_in, "1", "ERROR", "unable to find map class or invocation error for " + w + " / " + doctype);
                        errorcode = 3;
                        ex.printStackTrace();
                    }
                    
                    
           
        }  // each warehouse to receive 940
        
        // now lets set the order issourced flag if errorcode still equals 0
        if (errorcode == 0) {
           // OVData.updateOrderSourceFlag(nbr);
        }
        
        return errorcode;
     }
     
    public static int Create219(String nbr, String type)  {
        int errorcode = 0;
       
        // errorcode = 0 ... clean exit
        // errorcode = 1 ... no record found in wh_mstr for wh doctype
        // errorcode = 2 ... unable to retrieve carriers from order
        // errorcode = 3 ... any catch error below ...try running from command line to see trace dump
        // errorcode = 4 ... shipper does not exist
        
        
        ArrayList<String> cars = new ArrayList();
        String doctype = "219";
        String map = "";
        
        // lets determine the list of carriers to quote for this freight order
        // first we need the proposed carrier or carrier list to send quote to
        cars = OVData.getFreightOrderCarrierList(nbr);
        if (cars.size() == 0) {
            return 2;
        }
        
         
        for (String ca : cars) {   
            
        
        map = OVData.getEDIOutMap(ca, doctype); 
        String[] c_in = initEDIControl();   // controlarray in this order : entity, doctype, map, filename, isacontrolnum, gsctrlnum, stctrlnum, ref ; 
        
        c_in[0] = ca;
        c_in[1] = doctype;
        c_in[2] = map;
        c_in[3] = "";
        c_in[4] = "";
        c_in[5] = "";
        c_in[6] = "";
        c_in[7] = nbr;
        c_in[15] = "0"; // dir out
        c_in[12] = "0"; // is override
        
        // get Delimiters from Cust Defaults
        String[] defaults = OVData.getEDIOutCustDefaults(ca, doctype, "0");
        c_in[9] = defaults[7]; 
        c_in[10] = defaults[6]; 
        c_in[11] = defaults[8]; 
        
        if (map.isEmpty()) {
            OVData.writeEDILog(c_in, "1", "ERROR", "no edi_mstr map for " + ca + " / " + doctype);
            errorcode = 1;
                   continue;
        } 
        
         // Mapdata method call below requires two parameters (ArrayList, String[]) ...doc and c
        ArrayList doc = new ArrayList();
        doc.add(nbr);
        
                    try {
                     Class cls = Class.forName(map);
                    Object obj = cls.newInstance();
                    Method method = cls.getDeclaredMethod("Mapdata", ArrayList.class, String[].class);
               
                    
                    Object envelope = method.invoke(obj, doc, c_in); // envelope array holds in this order (isa, gs, ge, iea, filename, isactrlnum, gsctrlnum, stctrlnum)
                    String[] c_out = (String[])envelope;
                    
                    OVData.writeEDILog(c_out, "1", "INFO", "Export"); 
                    OVData.CreateFreightEDIRecs(c_in, nbr);

                    } catch (IllegalAccessException | ClassNotFoundException |
                             InstantiationException | NoSuchMethodException |
                            InvocationTargetException ex) {
                        OVData.writeEDILog(c_in, "1", "ERROR", "unable to find map class or invocation error for " + ca + " / " + doctype);
                        errorcode = 3;
                        ex.printStackTrace();
                    }
        
                   
        
                   
           
        }  // each warehouse to receive 940
        
        // now lets set the order issourced flag if errorcode still equals 0
        if (errorcode == 0) {
           // OVData.updateOrderSourceFlag(nbr);
        }
        
        return errorcode;
     }
      
    public static int Create990o(String nbr, String response)  {
        int errorcode = 0;
       
        // errorcode = 0 ... clean exit
        // errorcode = 1 ... no record found in wh_mstr for wh doctype
        // errorcode = 2 ... unable to retrieve carriers from order
        // errorcode = 3 ... any catch error below ...try running from command line to see trace dump
        // errorcode = 4 ... shipper does not exist
        
        
        
        String doctype = "990";
        String map = "";
        
        
      
        
         String tp = OVData.getFreightOrderCarrierAssigned(nbr);
       
            
        
        map = OVData.getEDIOutMap(tp, doctype); 
        
        String[] c_in = initEDIControl();   // controlarray in this order : entity, doctype, map, filename, isacontrolnum, gsctrlnum, stctrlnum, ref ; 
        
         c_in[0] = tp;
        c_in[1] = doctype;
        c_in[2] = map;
        c_in[3] = "";
        c_in[4] = "";
        c_in[5] = "";
        c_in[6] = "";
        c_in[7] = nbr;
        c_in[15] = "0"; // dir out
        c_in[12] = "0"; // is override
        
        // get Delimiters from Cust Defaults
        String[] defaults = OVData.getEDIOutCustDefaults(tp, doctype, "0");
        c_in[9] = defaults[7]; 
        c_in[10] = defaults[6]; 
        c_in[11] = defaults[8]; 
        
        if (tp.isEmpty()) {
            OVData.writeEDILog(c_in, "1", "ERROR", "no carrier for this freight order " + tp + " / " + doctype);
            errorcode = 1;
            return errorcode;
        } 
        
        if (map.isEmpty()) {
            OVData.writeEDILog(c_in, "1", "ERROR", "no edi_mstr map for " + tp + " / " + doctype);
            errorcode = 1;
            return errorcode;
        } 
        
        // Mapdata method call below requires two parameters (ArrayList, String[]) ...doc and c
        ArrayList doc = new ArrayList();
        doc.add(nbr);
        
                    try {
                   Class cls = Class.forName(map);
                    Object obj = cls.newInstance();
                    Method method = cls.getDeclaredMethod("Mapdata", ArrayList.class, String[].class);
               
                    
                    Object envelope = method.invoke(obj, doc, c_in); // envelope array holds in this order (isa, gs, ge, iea, filename, isactrlnum, gsctrlnum, stctrlnum)
                    String[] c_out = (String[])envelope;
                    
                    OVData.writeEDILog(c_out, "1", "INFO", "Export"); 
                    OVData.CreateFreightEDIRecs(c_out, nbr);

                    } catch (IllegalAccessException | ClassNotFoundException |
                             InstantiationException | NoSuchMethodException |
                            InvocationTargetException ex) {
                        OVData.writeEDILog(c_in, "1", "ERROR", "unable to find map class or invocation error for " + tp + " / " + doctype);
                        errorcode = 3;
                        ex.printStackTrace();
                    }
           
     
        
        // now lets set the order issourced flag if errorcode still equals 0
        if (errorcode == 0) {
           // OVData.updateOrderSourceFlag(nbr);
        }
        
        return errorcode;
     }
      
    public static int Create204(String nbr)  {
        int errorcode = 0;
       
        // errorcode = 0 ... clean exit
        // errorcode = 1 ... no record found in wh_mstr for wh doctype
        // errorcode = 2 ... unable to retrieve carriers from order
        // errorcode = 3 ... any catch error below ...try running from command line to see trace dump
        // errorcode = 4 ... shipper does not exist
        
        
        
        String doctype = "204";
        String map = "";
      
        
         String ca = OVData.getFreightOrderCarrierAssigned(nbr);
       
            
        
        map = OVData.getEDIOutMap(ca, doctype);
        
        
         String[] c_in = initEDIControl();   // controlarray in this order : entity, doctype, map, filename, isacontrolnum, gsctrlnum, stctrlnum, ref ; 
        
        c_in[0] = ca;
        c_in[1] = doctype;
        c_in[2] = map;
        c_in[3] = "";
        c_in[4] = "";
        c_in[5] = "";
        c_in[6] = "";
        c_in[7] = nbr;
        c_in[15] = "0"; // dir out
        c_in[12] = "0"; // is override
        
        // get Delimiters from Cust Defaults
        String[] defaults = OVData.getEDIOutCustDefaults(ca, doctype, "0");
        c_in[9] = defaults[7]; 
        c_in[10] = defaults[6]; 
        c_in[11] = defaults[8]; 
        
        
        if (map.isEmpty()) {
            OVData.writeEDILog(c_in, "1", "ERROR", "no edi_mstr map for " + c_in + " / " + doctype);
            errorcode = 1;
            return errorcode;
        } 
        
         // Mapdata method call below requires two parameters (ArrayList, String[]) ...doc and c
        ArrayList doc = new ArrayList();
        doc.add(nbr);
        
        
                    try {
                    Class cls = Class.forName(map);
                    Object obj = cls.newInstance();
                    Method method = cls.getDeclaredMethod("Mapdata", ArrayList.class, String[].class);
               
                    
                    Object envelope = method.invoke(obj, doc, c_in); // envelope array holds in this order (isa, gs, ge, iea, filename, isactrlnum, gsctrlnum, stctrlnum)
                    String[] c_out = (String[])envelope;
                    
                    OVData.writeEDILog(c_out, "1", "INFO", "Export"); 
                    OVData.CreateFreightEDIRecs(c_out, nbr); 

                    } catch (IllegalAccessException | ClassNotFoundException |
                             InstantiationException | NoSuchMethodException |
                            InvocationTargetException ex) {
                        OVData.writeEDILog(c_in, "1", "ERROR", "unable to find map class or invocation error for " + c_in + " / " + doctype);
                        errorcode = 3;
                        ex.printStackTrace();
                    }
           
     
        
        // now lets set the order issourced flag if errorcode still equals 0
        if (errorcode == 0) {
           // OVData.updateOrderSourceFlag(nbr);
        }
        
        return errorcode;
     }
       
      
     // miscellaneous 
      public static String[] generateEnvelope(String entity, String doctype, String dir) {
        
        String [] envelope = new String[7];  // will hold 7 elements.... ISA, GS, GE,IEA, filename, isactrl, gsctrl
        
        //  * @return Array with 0=ISA, 1=ISAQUAL, 2=GS, 3=BS_ISA, 4=BS_ISA_QUAL, 5=BS_GS, 6=ELEMDELIM, 7=SEGDELIM, 8=SUBDELIM, 9=FILEPATH, 10=FILEPREFIX, 11=FILESUFFIX,
        //  * @return 12=X12VERSION, 13=SUPPCODE, 14=DIRECTION
        String[] defaults = OVData.getEDIOutCustDefaults(entity, doctype, dir);
       
        
        // get counter for ediout
        int filenumber = OVData.getNextNbr("ediout");
        
       int sdint = Integer.valueOf(defaults[7]);
       int edint = Integer.valueOf(defaults[6]);
       int udint = Integer.valueOf(defaults[8]);
         String sd = Character.toString((char) sdint );   // "\n"  or "\u001c"
         String ed = Character.toString((char) edint );
         String ud = Character.toString((char) udint );
         
         
       //  if default filename is empty..set as generic
         if (defaults[10].isEmpty()) {
             defaults[10] = "generic";
         }
         
         String filename = defaults[10] + "." + String.valueOf(filenumber) + "." + defaults[11];
        
         //  if filepath is defined...use this for explicit file path relative to root
         if (! defaults[9].isEmpty()) {
             filename = defaults[9] + "/" + filename;
         }
         
         //File f = new File(defaults[9] + defaults[10] + "." + String.valueOf(filenumber) + "." + defaults[11]);
         //BufferedWriter output;
         //output = new BufferedWriter(new FileWriter(f));
         DateFormat isadfdate = new SimpleDateFormat("yyMMdd");
         DateFormat gsdfdate = new SimpleDateFormat("yyyyMMdd");
         DateFormat isadftime = new SimpleDateFormat("HHmm");
         DateFormat gsdftime = new SimpleDateFormat("HHmm");
         Date now = new Date();
        
         
         String isa1 = "00";
         String isa2 = "          ";
         String isa3 = "00";
         String isa4 = "          ";
         String isa5 = String.format("%2s", defaults[4]);
         String isa6 = String.format("%-15s", defaults[3]);
         String isa7 = String.format("%2s", defaults[1]);
         String isa8 = String.format("%-15s", defaults[0]);
         String isa9 = isadfdate.format(now);
         String isa10 = isadftime.format(now);
         String isa11 = "U";
         String isa12 = defaults[12].substring(0,5);
         String isa13 = String.format("%09d", filenumber);
         String isa14 = "0";
         String isa15 = "P";
         String isa16 = ud;
         
         String gs1 = "";
         switch(doctype) {
             case "810" :
                 gs1 = "IN";
                 break;
             case "856" :
                 gs1 = "SH";
                 break;
             case "940" :
                 gs1 = "OW";
                 break;
             case "943" :
                 gs1 = "AR";
                 break;
             case "990" :
                 gs1 = "GF";
                 break;
             default :
                 gs1 = "XX";
         }
        
         
         String gs2 = defaults[5];
         String gs3 = defaults[2];
         String gs4 = gsdfdate.format(now);
         String gs5 = gsdftime.format(now);
         String gs6 = String.valueOf(filenumber);
         String gs7 = "X";
         String gs8 = defaults[12];
          
        
        
       
          
           envelope[0] = "ISA" + ed + isa1 + ed + isa2 + ed + isa3 + ed + isa4 + ed + isa5 + ed + isa6 + ed + isa7 + ed + isa8 + ed + isa9 + ed + 
                 isa10 + ed + isa11 + ed + isa12 + ed + isa13 + ed + isa14 + ed + isa15 + ed + isa16 ;
           envelope[0] = envelope[0].toUpperCase();
           envelope[1] = "GS" + ed + gs1 + ed + gs2 + ed + gs3 + ed + gs4 + ed + gs5 + ed + gs6 + ed + gs7 + ed + gs8;
            envelope[1] = envelope[1].toUpperCase();
           envelope[2] = "GE" + ed + "1" + ed + String.valueOf(filenumber);
            envelope[2] = envelope[2].toUpperCase();
           envelope[3] = "IEA" + ed + "1" + ed + String.format("%09d", filenumber);
            envelope[3] = envelope[3].toUpperCase();
            
           envelope[4] = filename;
           envelope[5] = String.format("%09d", filenumber);
           envelope[6] = String.valueOf(filenumber);
           
            return envelope;
      }
      
       public static String[] generate997Envelope(String[] in_isa, String[] in_gs) {
        
        
                    
        String [] envelope = new String[7];  // will hold 7 elements.... ISA, GS, GE,IEA, filename, isactrl, gsctrl
        
        // get counter for ediout
        int filenumber = OVData.getNextNbr("ediout");
        
     
         String sd = "\n";   // "\n"  or "\u001c"
         String ed = "*";
         String ud = ">";
         
         
       //  String filename = defaults[9] + defaults[10] + "." + String.valueOf(filenumber) + "." + defaults[11];
         String filename = "997" + "." + in_isa[6].trim() + "." + String.valueOf(filenumber) + "." + "txt";
        
         //File f = new File(defaults[9] + defaults[10] + "." + String.valueOf(filenumber) + "." + defaults[11]);
         //BufferedWriter output;
         //output = new BufferedWriter(new FileWriter(f));
         DateFormat isadfdate = new SimpleDateFormat("yyMMdd");
         DateFormat gsdfdate = new SimpleDateFormat("yyyyMMdd");
         DateFormat isadftime = new SimpleDateFormat("HHmm");
         DateFormat gsdftime = new SimpleDateFormat("HHmm");
         Date now = new Date();
        
         
         String isa1 = "00";
         String isa2 = "          ";
         String isa3 = "00";
         String isa4 = "          ";
         String isa5 = String.format("%2s", in_isa[7]);
         String isa6 = String.format("%-15s", in_isa[8].trim());
         String isa7 = String.format("%2s", in_isa[5]);
         String isa8 = String.format("%-15s", in_isa[6].trim());
         String isa9 = isadfdate.format(now);
         String isa10 = isadftime.format(now);
         String isa11 = "U";
         String isa12 = in_gs[8].substring(0,5);
         String isa13 = String.format("%09d", filenumber);
         String isa14 = "0";
         String isa15 = "P";
         String isa16 = ud;
         
         String gs1 = "FA";
         
        
         
         String gs2 = in_gs[3];
         String gs3 = in_gs[2];
         String gs4 = gsdfdate.format(now);
         String gs5 = gsdftime.format(now);
         String gs6 = String.valueOf(filenumber);
         String gs7 = "X";
         String gs8 = in_gs[8];
          
        
        
       
          
           envelope[0] = "ISA" + ed + isa1 + ed + isa2 + ed + isa3 + ed + isa4 + ed + isa5 + ed + isa6 + ed + isa7 + ed + isa8 + ed + isa9 + ed + 
                 isa10 + ed + isa11 + ed + isa12 + ed + isa13 + ed + isa14 + ed + isa15 + ed + isa16 + sd ;
           envelope[0] = envelope[0].toUpperCase();
           envelope[1] = "GS" + ed + gs1 + ed + gs2 + ed + gs3 + ed + gs4 + ed + gs5 + ed + gs6 + ed + gs7 + ed + gs8 + sd;
            envelope[1] = envelope[1].toUpperCase();
           envelope[2] = "GE" + ed + "1" + ed + String.valueOf(filenumber) + sd;
            envelope[2] = envelope[2].toUpperCase();
           envelope[3] = "IEA" + ed + "1" + ed + String.format("%09d", filenumber) + sd;
            envelope[3] = envelope[3].toUpperCase();
            
          
           
            envelope[4] = filename;
            envelope[5] = String.format("%09d", filenumber);
            envelope[6] = String.valueOf(filenumber);
           
            return envelope;
      }
          
      public static String trimSegment(String segment, String delimiter) {
          String mystring = "";
          if ( delimiter.charAt(0) == segment.charAt(segment.length()- 1) ) {
              mystring = segment.substring(0, segment.length() - 1);
              if ( delimiter.charAt(0) == mystring.charAt(mystring.length()- 1) ) {
               return trimSegment(mystring, delimiter);
              }
          } else {
              mystring = segment;
          }
          return mystring;
      }
      
      public static String escapeDelimiter(String delim) {
      if (delim.equals("*")) {
            delim = "\\*";
        }
        if (delim.equals("^")) {
            delim = "\\^";
        }
        return delim;
      }
      
      public static String delimConvertIntToStr(String intdelim) {
        String delim = "";
        int x = Integer.valueOf(intdelim);
        delim = String.valueOf(Character.toString((char) x));
        return delim;
      }
      
      
      public static Boolean isEnvelopeSegment(String seg) {
      
      return (seg.equals("ISA") || seg.equals("GS") || 
              seg.equals("ST") || seg.equals("SE") ||
               seg.equals("GE") || seg.equals("IEA") ) ? true : false ;
      }
      
     public void writeFile(String filecontent, String dir, String filename) throws MalformedURLException, SmbException, IOException {
    
  // File folder = new File("smb://10.17.2.55/edi");
  // File[] listOfFiles = folder.listFiles();

    
 //   NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(null, null, null);
     
    NtlmPasswordAuthentication auth = NtlmPasswordAuthentication.ANONYMOUS;
    // if samba is used filepath should be something akin to smb://10.10.1.1/somepath/somedir/ + filename
    
   // SmbFile folder = new SmbFile("smb://10.17.2.55/edi/", auth);
    
    if (dir.isEmpty()) {
        dir = OVData.getEDIOutDir();
    }
    
    Path path = Paths.get(dir + "/" + filename);
    Path archpath = Paths.get(OVData.getEDIOutArch() + "/" + filename);
    
    
    BufferedWriter output;
    
         if (path.startsWith("smb")) {
         output = new BufferedWriter(new OutputStreamWriter(new SmbFileOutputStream(new SmbFile(path.toString(), auth))));
         output.write(filecontent);
         output.close();
         // now arch
         output = new BufferedWriter(new OutputStreamWriter(new SmbFileOutputStream(new SmbFile(archpath.toString(), auth))));
         output.write(filecontent);
         output.close();
         } else {
         output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path.toFile())));
         output.write(filecontent);
         output.close();  
         // now arch
         output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(archpath.toFile())));
         output.write(filecontent);
         output.close();   
         }
   // SmbFile[] listOfFiles = folder.listFiles();
         
        
    }
      
      public void writeFileCmdLine(String filecontent, String filename) throws MalformedURLException, SmbException, IOException {
    
//    File folder = new File("smb://10.17.2.55/edi");
  // File[] listOfFiles = folder.listFiles();

    
 //   NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(null, null, null);
     
    NtlmPasswordAuthentication auth = NtlmPasswordAuthentication.ANONYMOUS;
    // if samba is used filepath should be something akin to smb://10.10.1.1/somepath/somedir/ + filename
    
   // SmbFile folder = new SmbFile("smb://10.17.2.55/edi/", auth);
    
   
    
    Path path = Paths.get(filename);
    
    
    
    BufferedWriter output;
    
         if (path.startsWith("smb")) {
         output = new BufferedWriter(new OutputStreamWriter(new SmbFileOutputStream(new SmbFile(path.toString(), auth))));
         output.write(filecontent);
         output.close();
         } else {
         output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path.toFile(), true)));
         output.write(filecontent);
         output.close();  
         }
   // SmbFile[] listOfFiles = folder.listFiles();
         
        
    }
     
     
     public static class edi830 {
    
    public String isaSenderID = "";
    public String isaReceiverID = "";
    public String gsSenderID = "";
    public String gsReceiverID = "";
    public String isaCtrlNum = "";
    public String isaDate = "";
    public String doctype = "";
    public String docid = "";
    public String po = "";
    public String rlse = "";
    public String billto = "";
    public String shipto = "";
    public String ov_billto = "";
    public String ov_shipto = "";
    public String remarks = "";
    public String shipmethod = "";
    public String duedate = "";
    public String podate = "";
    public String st_name = "";
    public String st_line1 = "";
    public String st_line2 = "";
    public String st_line3 = "";
    public String st_city = "";
    public String st_state = "";
    public String st_zip = "";
    public String st_country = "";
    
       
    public ArrayList<String> detsku = new ArrayList<String>();
    public ArrayList<String> detcustitem = new ArrayList<String>();
    public ArrayList<String> detitem = new ArrayList<String>();
    public ArrayList<String> detqty = new ArrayList<String>();
    public ArrayList<String> detref = new ArrayList<String>();
    public ArrayList<String> detpo = new ArrayList<String>();
    public ArrayList<String> detline = new ArrayList<String>();
    public ArrayList<String> detlistprice = new ArrayList<String>();
    public ArrayList<String> detnetprice = new ArrayList<String>();
    public ArrayList<String> detdisc = new ArrayList<String>();
    public ArrayList<String> dettype = new ArrayList<String>();
    public ArrayList<String> detdate = new ArrayList<String>();
    
    
    public ArrayList<String> FSTloopqty = new ArrayList<String>();
    public ArrayList<String> FSTloopdate = new ArrayList<String>();
    public ArrayList<String> FSTlooptype = new ArrayList<String>();
    public ArrayList<String> FSTloopref = new ArrayList<String>();
    
    public Map<Integer, ArrayList<String>> map = new HashMap<Integer, ArrayList<String>>();
    
    
    
        public edi830() {
            
        }
        public edi830(String isasenderid, String isareceiverid, 
                      String gssenderid, String gsreceiverid,
                      String isactrlnum, String isadate, 
                      String doctype, String docid) {
            this.isaSenderID = isasenderid;
            this.isaReceiverID = isareceiverid;
            this.gsSenderID = gssenderid;
            this.gsReceiverID = gsreceiverid;
            this.isaCtrlNum = isactrlnum;
            this.isaDate = isadate;
            this.docid = docid;
            this.doctype = doctype;
        }
       
        public void setFSTMap(Integer i, ArrayList<String> a) {
            this.map.put(i, a);
        }
        
        public void setFSTLoopDate(String v) {
           this.FSTloopdate.add(v);
        }
        public void setFSTLoopQty(String v)  {
           this.FSTloopqty.add(v);
        }
        public void setFSTLoopType(String v) {
           this.FSTlooptype.add(v);
        }
        public void setFSTLoopRef(String v) {
           this.FSTloopref.add(v);
        }
        
        public void setDetDate(String v) {
           this.detdate.add(v);
        }
        public void setDetType(String v) {
           this.dettype.add(v);
        }
        public void setDetItem(String v) {
           this.detitem.add(v);
        }
        public void setDetCustItem(String v) {
           this.detcustitem.add(v);
        }
        public void setDetSku(String v) {
           this.detsku.add(v);
        }
        public void setDetRef(String v) {
           this.detref.add(v);
        }
        public void setDetPO(String v) {
           this.detpo.add(v);
        }
        public void setDetLine(String v) {
           this.detline.add(v);
        }
        public void setDetQty(String v) {
           this.detqty.add(v);
        }
        public void setDetListPrice(String v) {
           this.detlistprice.add(v);
        }
        public void setDetNetPrice(String v) {
           this.detnetprice.add(v);
        }
        public void setDetDisc(String v) {
           this.detdisc.add(v);
        }
       
        public void setRlse(String v) {
           this.rlse = v;
        }  
        public void setPO(String v) {
           this.po = v;
        }
        public void setPODate(String v) {
           this.podate = v;
        }
        public void setDueDate(String v) {
           this.duedate = v;
        }
        
        public void setRemarks(String v) {
           this.remarks = v;
        }
         public void setShipVia(String v) {
           this.shipmethod = v;
        }
        
        
        public void setShipTo(String v) {
           this.shipto = v;
        }
        public void setShipToName(String v) {
           this.st_name = v;
        }
        public void setShipToLine1(String v) {
           this.st_line1 = v;
        }
        public void setShipToLine2(String v) {
           this.st_line2 = v;
        }
        public void setShipToLine3(String v) {
           this.st_line3 = v;
        }
        public void setShipToCity(String v) {
           this.st_city = v;
        }
        public void setShipToState(String v) {
           this.st_state = v;
        }
        public void setShipToZip(String v) {
           this.st_zip = v;
        }
        public void setShipToCountry(String v) {
           this.st_country = v;
        }
        public void setBillTo(String v) {
           this.billto = v;
        }
        public void setOVShipTo(String v) {
            this.ov_shipto = v;
        }
        public void setOVBillTo(String v) {
            this.ov_billto = v;
        }
        public String getPO() {
           return this.po;
        }
        public Map getFSAMap(int i) {
           return this.map;
        }
        public String getRlse() {
           return this.rlse;
        }
        public String getPODate() {
           return this.podate;
        }
        public String getShipTo() {
           return this.shipto;
        }
        public String getShipToName() {
           return this.st_name;
        }
        public String getShipToLine1() {
           return this.st_line1;
        }
        public String getShipToLine2() {
           return this.st_line2;
        }
        public String getShipToLine3() {
           return this.st_line3;
        }
        public String getShipToCity() {
           return this.st_city;
        }
        public String getShipToState() {
           return this.st_state;
        }
        public String getShipToZip() {
           return this.st_zip;
        }
        public String getShipToCountry() {
           return this.st_country;
        }
        public String getBillTo() {
           return this.billto;
        }
        public String getOVShipTo() {
           return this.ov_shipto;
        }
        public String getOVBillTo() {
           return this.ov_billto;
        }
        public String getISASenderID() {
           return this.isaSenderID;
        }
        public String getISAReceiverID() {
           return this.isaReceiverID;
        }
        public String getDocType() {
           return this.doctype;
        }
        public String getDocID() {
           return this.docid;
        }

        public String getIsaSenderID() {
            return isaSenderID;
        }

        public String getFSTLoopDate(int i) {
            return FSTloopdate.get(i);
        }
        public String getFSTLoopQty(int i) {
            return FSTloopqty.get(i);
        }
        public String getFSTLoopType(int i) {
            return FSTlooptype.get(i);
        }
         public String getFSTLoopRef(int i) {
            return FSTloopref.get(i);
        }
        
        public String getDetDate(int i) {
            return detdate.get(i);
        }
        public String getDetType(int i) {
            return dettype.get(i);
        }
        public String getDetItem(int i) {
            return detitem.get(i);
        }
        public String getDetCustItem(int i) {
           return detcustitem.get(i);
        }
        public String getDetSku(int i) {
          return detsku.get(i);
        }
        public String getDetRef(int i) {
           return detref.get(i);
        }
        public String getDetPO(int i) {
           return detpo.get(i);
        }
        public String getDetLine(int i) {
           return detline.get(i);
        }
        public String getDetQty(int i) {
           return detqty.get(i);
        }
        public String getDetListPrice(int i) {
           return detlistprice.get(i);
        }
        public String getDetNetPrice(int i) {
           return detnetprice.get(i);
        }
        public String getDetDisc(int i) {
           return detdisc.get(i);
        }
                
        public int getDetCount() {
            return detitem.size();
        }
       
        public int getFCSCount() {
            return FSTloopdate.size();
        }
        
    }
     
    public static class edi850 {
    // Header fields
    public String isaSenderID = "";
    public String isaReceiverID = "";
    public String gsSenderID = "";
    public String gsReceiverID = "";
    public String isaCtrlNum = "";
    public String isaDate = "";
    public String doctype = "";
    public String docid = "";
    public String po = "";
    public String billto = "";
    public String shipto = "";
    public String ov_billto = "";
    public String ov_shipto = "";
    public String remarks = "";
    public String shipmethod = "";
    public String duedate = "";
    public String podate = "";
    public String st_name = "";
    public String st_line1 = "";
    public String st_line2 = "";
    public String st_line3 = "";
    public String st_city = "";
    public String st_state = "";
    public String st_zip = "";
    public String st_country = "";
    
    // Detail fields      
    public ArrayList<String> detsku = new ArrayList<String>();
    public ArrayList<String> detcustitem = new ArrayList<String>();
    public ArrayList<String> detitem = new ArrayList<String>();
    public ArrayList<String> detuom = new ArrayList<String>();
    public ArrayList<String> detqty = new ArrayList<String>();
    public ArrayList<String> detref = new ArrayList<String>();
    public ArrayList<String> detpo = new ArrayList<String>();
    public ArrayList<String> detline = new ArrayList<String>();
    public ArrayList<String> detlistprice = new ArrayList<String>();
    public ArrayList<String> detnetprice = new ArrayList<String>();
    public ArrayList<String> detdisc = new ArrayList<String>();
        
        public edi850() {
            
        }
        
        public edi850(String isasenderid, String isareceiverid, 
                      String gssenderid, String gsreceiverid,
                      String isactrlnum, String isadate, 
                      String doctype, String docid) {
            this.isaSenderID = isasenderid;
            this.isaReceiverID = isareceiverid;
            this.gsSenderID = gssenderid;
            this.gsReceiverID = gsreceiverid;
            this.isaCtrlNum = isactrlnum;
            this.isaDate = isadate;
            this.docid = docid;
            this.doctype = doctype;
        }
        
        
        public void addDetail() {
            this.detitem.add("");
            this.detuom.add("");
            this.detcustitem.add("");
            this.detsku.add("");
            this.detref.add("");
            this.detpo.add("");
            this.detline.add("");
            this.detqty.add("0");
            this.detlistprice.add("0");
            this.detnetprice.add("0");
            this.detdisc.add("0");
        }
        
        public void setDetItem(int i, String v) {
           this.detitem.set(i,v);
        }
        public void setDetUOM(int i, String v) {
           this.detuom.set(i,v);
        }
        public void setDetCustItem(int i, String v) {
           this.detcustitem.set(i,v);
        }
        public void setDetSku(int i, String v) {
           this.detsku.set(i,v);
        }
        public void setDetRef(int i, String v) {
           this.detref.set(i,v);
        }
        public void setDetPO(int i, String v) {
           this.detpo.set(i,v);
        }
        public void setDetLine(int i, String v) {
           this.detline.set(i,v);
        }
        public void setDetQty(int i, String v) {
           this.detqty.set(i,v);
        }
        public void setDetListPrice(int i, String v) {
           this.detlistprice.set(i,v);
        }
         public void setDetNetPrice(int i, String v) {
           this.detnetprice.set(i,v);
        }
          public void setDetDisc(int i, String v) {
           this.detdisc.set(i,v);
        }
        public void setPO(String v) {
           this.po = v;
        }
        public void setPODate(String v) {
           this.podate = v;
        }
         public void setDueDate(String v) {
           this.duedate = v;
        }
        public void setShipTo(String v) {
           this.shipto = v;
        }
         public void setShipVia(String v) {
           this.shipmethod = v;
        }
          public void setRemarks(String v) {
           this.remarks = v;
        }
        public void setShipToName(String v) {
           this.st_name = v;
        }
        public void setShipToLine1(String v) {
           this.st_line1 = v;
        }
        public void setShipToLine2(String v) {
           this.st_line2 = v;
        }
        public void setShipToLine3(String v) {
           this.st_line3 = v;
        }
        public void setShipToCity(String v) {
           this.st_city = v;
        }
        public void setShipToState(String v) {
           this.st_state = v;
        }
        public void setShipToZip(String v) {
           this.st_zip = v;
        }
        public void setShipToCountry(String v) {
           this.st_country = v;
        }
        public void setBillTo(String v) {
           this.billto = v;
        }
        public void setOVShipTo(String v) {
            this.ov_shipto = v;
        }
        public void setOVBillTo(String v) {
            this.ov_billto = v;
        }
        public String getPO() {
           return this.po;
        }
        public String getPODate() {
           return this.podate;
        }
        public String getShipTo() {
           return this.shipto;
        }
          public String getRemarks() {
           return this.remarks;
        }
            public String getShipVia() {
           return this.shipmethod;
        }
        public String getShipToName() {
           return this.st_name;
        }
        public String getShipToLine1() {
           return this.st_line1;
        }
        public String getShipToLine2() {
           return this.st_line2;
        }
        public String getShipToLine3() {
           return this.st_line3;
        }
        public String getShipToCity() {
           return this.st_city;
        }
        public String getShipToState() {
           return this.st_state;
        }
        public String getShipToZip() {
           return this.st_zip;
        }
        public String getShipToCountry() {
           return this.st_country;
        }
        public String getBillTo() {
           return this.billto;
        }
        public String getOVShipTo() {
           return this.ov_shipto;
        }
        public String getOVBillTo() {
           return this.ov_billto;
        }
        public String getISASenderID() {
           return this.isaSenderID;
        }
        public String getISAReceiverID() {
           return this.isaReceiverID;
        }
        public String getDocType() {
           return this.doctype;
        }
        public String getDocID() {
           return this.docid;
        }
         public String getHdrDueDate() {
           return this.duedate;
        }

        public String getIsaSenderID() {
            return isaSenderID;
        }

        public String getDetItem(int i) {
            return detitem.get(i);
        }
        public String getDetUOM(int i) {
            return detuom.get(i);
        }
        public String getDetCustItem(int i) {
           return detcustitem.get(i);
        }
        public String getDetSku(int i) {
          return detsku.get(i);
        }
        public String getDetRef(int i) {
           return detref.get(i);
        }
        public String getDetPO(int i) {
           return detpo.get(i);
        }
        public String getDetLine(int i) {
           return detline.get(i);
        }
        public String getDetQty(int i) {
           return detqty.get(i);
        }
        public String getDetListPrice(int i) {
           return detlistprice.get(i);
        }
        public String getDetNetPrice(int i) {
           return detnetprice.get(i);
        }
        public String getDetDisc(int i) {
           return detdisc.get(i);
        }
        
        
        public int getDetCount() {
            return detitem.size();
        }
       
        
    }
    
     public static class edi945 {
    // Header fields
    public String isaSenderID = "";
    public String isaReceiverID = "";
    public String gsSenderID = "";
    public String gsReceiverID = "";
    public String isaCtrlNum = "";
    public String isaDate = "";
    public String doctype = "";
    public String docid = "";
    public String po = "";
    public String so = "";
    public String shipper = "";
    public String billto = "";
    public String shipto = "";
    public String ov_billto = "";
    public String ov_shipto = "";
    public String remarks = "";
    public String shipmethod = "";
    public String duedate = "";
    public String shipdate = "";
    public String podate = "";
    public String st_name = "";
    public String st_line1 = "";
    public String st_line2 = "";
    public String st_line3 = "";
    public String st_city = "";
    public String st_state = "";
    public String st_zip = "";
    public String st_country = "";
    
    // Detail fields     
    public ArrayList<String[]> detailArray = new ArrayList<String[]>();
    public int DetFieldsCount945 = 17;
    public String[] initDetailArray(String[] a) {
        for (int i = 0; i < a.length; i++) {
            a[i] = "";
        }        
        return a;
    }
   
        
        public edi945() {
            
        }
        
        public edi945(String isasenderid, String isareceiverid, 
                      String gssenderid, String gsreceiverid,
                      String isactrlnum, String isadate, 
                      String doctype, String docid) {
            this.isaSenderID = isasenderid;
            this.isaReceiverID = isareceiverid;
            this.gsSenderID = gssenderid;
            this.gsReceiverID = gsreceiverid;
            this.isaCtrlNum = isactrlnum;
            this.isaDate = isadate;
            this.docid = docid;
            this.doctype = doctype;
        }
        
        // setters for detail
        public void setDetLine(int i, String v) {
          this.detailArray.get(i)[0] = v;
        }
        public void setDetItem(int i, String v) {
         this.detailArray.get(i)[1] = v;
        }
        public void setDetCustItem(int i, String v) {
          this.detailArray.get(i)[2] = v;
        }
        public void setDetDesc(int i, String v) {
           this.detailArray.get(i)[3] = v;
        }
        public void setDetSite(int i, String v) {
           this.detailArray.get(i)[4] = v;
        }
        public void setDetQtyOrd(int i, String v) {
          this.detailArray.get(i)[5] = v;
        }
        public void setDetQtyShp(int i, String v) {
           this.detailArray.get(i)[6] = v;
        }
        public void setDetListPrice(int i, String v) {
           this.detailArray.get(i)[7] = v;
        }
         public void setDetNetPrice(int i, String v) {
           this.detailArray.get(i)[8] = v;
        }
          public void setDetNetWt(int i, String v) {
           this.detailArray.get(i)[9] = v;
        }
        public void setDetWH(int i, String v) {
           this.detailArray.get(i)[10] = v;
        }
        public void setDetLoc(int i, String v) {
          this.detailArray.get(i)[11] = v;
        }
        public void setDetUOM(int i, String v) {
          this.detailArray.get(i)[12] = v;
        }        
        public void setDetSku(int i, String v) {
           this.detailArray.get(i)[13] = v;
        }
        public void setDetRef(int i, String v) {
         this.detailArray.get(i)[14] = v;
        }
        public void setDetPO(int i, String v) {
          this.detailArray.get(i)[15] = v;
        }
        public void setDetDisc(int i, String v) {
           this.detailArray.get(i)[16] = v;
        }
        
        
       // getters for detail
         public String getDetLine(int i) {
            return detailArray.get(i)[0];
        }
          public String getDetItem(int i) {
            return detailArray.get(i)[1];
        }
         public String getDetCustItem(int i) {
           return detailArray.get(i)[2];
        }
         public String getDetDesc(int i) {
            return detailArray.get(i)[3];
        }
          public String getDetSite(int i) {
            return detailArray.get(i)[4];
        }
          public String getDetQtyOrd(int i) {
           return detailArray.get(i)[5];
        }
        public String getDetQtyShp(int i) {
           return detailArray.get(i)[6];
        }
        public String getDetListPrice(int i) {
           return detailArray.get(i)[7];
        }
        public String getDetNetPrice(int i) {
           return detailArray.get(i)[8];
        }
        public String getDetNetWt(int i) {
           return detailArray.get(i)[9];
        }
           public String getDetWH(int i) {
            return detailArray.get(i)[10];
        }
            public String getDetLoc(int i) {
            return detailArray.get(i)[11];
        }
        public String getDetUOM(int i) {
            return detailArray.get(i)[12];
        }
        public String getDetSku(int i) {
          return detailArray.get(i)[13];
        }
        public String getDetRef(int i) {
           return detailArray.get(i)[14];
        }
        public String getDetPO(int i) {
           return detailArray.get(i)[15];
        }
        public String getDetDisc(int i) {
           return detailArray.get(i)[16];
        } 
        
        
        
        
        
       // header setters 
        
        public void setPO(String v) {
           this.po = v;
        }
        public void setSO(String v) {
           this.so = v;
        }
        public void setShipper(String v) {
           this.shipper = v;
        }
        public void setPODate(String v) {
           this.podate = v;
        }
         public void setDueDate(String v) {
           this.duedate = v;
        }
          public void setShipDate(String v) {
           this.shipdate = v;
        }
        public void setShipTo(String v) {
           this.shipto = v;
        }
         public void setShipVia(String v) {
           this.shipmethod = v;
        }
          public void setRemarks(String v) {
           this.remarks = v;
        }
        public void setShipToName(String v) {
           this.st_name = v;
        }
        public void setShipToLine1(String v) {
           this.st_line1 = v;
        }
        public void setShipToLine2(String v) {
           this.st_line2 = v;
        }
        public void setShipToLine3(String v) {
           this.st_line3 = v;
        }
        public void setShipToCity(String v) {
           this.st_city = v;
        }
        public void setShipToState(String v) {
           this.st_state = v;
        }
        public void setShipToZip(String v) {
           this.st_zip = v;
        }
        public void setShipToCountry(String v) {
           this.st_country = v;
        }
        public void setBillTo(String v) {
           this.billto = v;
        }
        public void setOVShipTo(String v) {
            this.ov_shipto = v;
        }
        public void setOVBillTo(String v) {
            this.ov_billto = v;
        }
        
        // header getters
        public String getPO() {
           return this.po;
        }
        public String getSO() {
           return this.so;
        }
        public String getShipper() {
           return this.shipper;
        }
        public String getPODate() {
           return this.podate;
        }
        public String getShipTo() {
           return this.shipto;
        }
          public String getRemarks() {
           return this.remarks;
        }
            public String getShipVia() {
           return this.shipmethod;
        }
        public String getShipToName() {
           return this.st_name;
        }
        public String getShipToLine1() {
           return this.st_line1;
        }
        public String getShipToLine2() {
           return this.st_line2;
        }
        public String getShipToLine3() {
           return this.st_line3;
        }
        public String getShipToCity() {
           return this.st_city;
        }
        public String getShipToState() {
           return this.st_state;
        }
        public String getShipToZip() {
           return this.st_zip;
        }
        public String getShipToCountry() {
           return this.st_country;
        }
        public String getBillTo() {
           return this.billto;
        }
        public String getOVShipTo() {
           return this.ov_shipto;
        }
        public String getOVBillTo() {
           return this.ov_billto;
        }
        public String getISASenderID() {
           return this.isaSenderID;
        }
        public String getISAReceiverID() {
           return this.isaReceiverID;
        }
        public String getDocType() {
           return this.doctype;
        }
        public String getDocID() {
           return this.docid;
        }
         public String getHdrDueDate() {
           return this.duedate;
        }
          public String getShipDate() {
           return this.shipdate;
        }

        public String getIsaSenderID() {
            return isaSenderID;
        }

      
        
        
        public int getDetCount() {
            return detailArray.size();
        }
       
        
    }
    
      public static class edi204i {
    // Header fields
    public String isaSenderID = "";
    public String isaReceiverID = "";
    public String gsSenderID = "";
    public String gsReceiverID = "";
    public String isaCtrlNum = "";
    public String isaDate = "";
    public String doctype = "";
    public String docid = "";
    public String custfo = "";
    public String carrier = "";
    public String equiptype = "";
    public String cust = "";
    public String tpid = "";
    public String ref = "";
    public String remarks = "";
    public String shipmethod = "";
    public String fodate = "";
    public String bol = "";
    public String weight = "";
    
    // Detail fields     
    public ArrayList<String[]> detailArray = new ArrayList<String[]>();
    public int DetFieldsCount204i = 22;
    public String[] initDetailArray(String[] a) {
        for (int i = 0; i < a.length; i++) {
            a[i] = "";
        }        
        return a;
    }
   
        
        public edi204i() {
            
        }
        
        public edi204i(String isasenderid, String isareceiverid, 
                      String gssenderid, String gsreceiverid,
                      String isactrlnum, String isadate, 
                      String doctype, String docid) {
            this.isaSenderID = isasenderid;
            this.isaReceiverID = isareceiverid;
            this.gsSenderID = gssenderid;
            this.gsReceiverID = gsreceiverid;
            this.isaCtrlNum = isactrlnum;
            this.isaDate = isadate;
            this.docid = docid;
            this.doctype = doctype;
        }
        
        // setters for detail
        public void setDetLine(int i, String v) {
          this.detailArray.get(i)[0] = v;
        }
        public void setDetType(int i, String v) {
         this.detailArray.get(i)[1] = v;
        }
        public void setDetShipper(int i, String v) {
          this.detailArray.get(i)[2] = v;
        }
        public void setDetShipDate(int i, String v) {
           this.detailArray.get(i)[3] = v;
        }
        public void setDetShipTime(int i, String v) {
           this.detailArray.get(i)[4] = v;
        }
        public void setDetDelvDate(int i, String v) {
          this.detailArray.get(i)[5] = v;
        }
        public void setDetDelvTime(int i, String v) {
           this.detailArray.get(i)[6] = v;
        }
        public void setDetRef(int i, String v) {
           this.detailArray.get(i)[7] = v;
        }
         public void setDetAddrCode(int i, String v) {
           this.detailArray.get(i)[8] = v;
        }
          public void setDetAddrName(int i, String v) {
           this.detailArray.get(i)[9] = v;
        }
        public void setDetAddrLine1(int i, String v) {
           this.detailArray.get(i)[10] = v;
        }
        public void setDetAddrLine2(int i, String v) {
          this.detailArray.get(i)[11] = v;
        }
        public void setDetAddrCity(int i, String v) {
          this.detailArray.get(i)[12] = v;
        }        
        public void setDetAddrState(int i, String v) {
           this.detailArray.get(i)[13] = v;
        }
        public void setDetAddrZip(int i, String v) {
         this.detailArray.get(i)[14] = v;
        }
        public void setDetAddrContact(int i, String v) {
          this.detailArray.get(i)[15] = v;
        }
        public void setDetAddrPhone(int i, String v) {
           this.detailArray.get(i)[16] = v;
        }
        public void setDetUnits(int i, String v) {
           this.detailArray.get(i)[17] = v;
        }
        public void setDetBoxes(int i, String v) {
           this.detailArray.get(i)[18] = v;
        }
        public void setDetWeight(int i, String v) {
           this.detailArray.get(i)[19] = v;
        }
        public void setDetWeightUOM(int i, String v) {
           this.detailArray.get(i)[20] = v;
        }
        public void setDetRemarks(int i, String v) {
           this.detailArray.get(i)[21] = v;
        }
        
        
       // getters for detail
         public String getDetLine(int i) {
            return detailArray.get(i)[0];
        }
          public String getDetType(int i) {
            return detailArray.get(i)[1];
        }
         public String getDetShipper(int i) {
           return detailArray.get(i)[2];
        }
         public String getDetShipDate(int i) {
            return detailArray.get(i)[3];
        }
          public String getDetShipTime(int i) {
            return detailArray.get(i)[4];
        }
          public String getDetDelvDate(int i) {
           return detailArray.get(i)[5];
        }
        public String getDetDelvTime(int i) {
           return detailArray.get(i)[6];
        }
        public String getDetRef(int i) {
           return detailArray.get(i)[7];
        }
        public String getDetAddrCode(int i) {
           return detailArray.get(i)[8];
        }
        public String getDetAddrName(int i) {
           return detailArray.get(i)[9];
        }
           public String getDetAddrLine1(int i) {
            return detailArray.get(i)[10];
        }
            public String getDetAddrLine2(int i) {
            return detailArray.get(i)[11];
        }
        public String getDetAddrCity(int i) {
            return detailArray.get(i)[12];
        }
        public String getDetAddrState(int i) {
          return detailArray.get(i)[13];
        }
        public String getDetAddrZip(int i) {
           return detailArray.get(i)[14];
        }
        public String getDetAddrContact(int i) {
           return detailArray.get(i)[15];
        }
        public String getDetAddrPhone(int i) {
           return detailArray.get(i)[16];
        } 
        public String getDetUnits(int i) {
           return detailArray.get(i)[17];
        }
        public String getDetBoxes(int i) {
           return detailArray.get(i)[18];
        }
        public String getDetWeight(int i) {
           return detailArray.get(i)[19];
        }
        public String getDetWeightUOM(int i) {
           return detailArray.get(i)[20];
        }
        public String getDetRemarks(int i) {
           return detailArray.get(i)[21];
        }
       // header setters 
   
    
        public void setCustFO(String v) {
           this.custfo = v;
        }
        public void setCarrier(String v) {
           this.carrier = v;
        }
        public void setEquipType(String v) {
           this.equiptype = v;
        }
        public void setCust(String v) {
           this.cust = v;
        }
        public void setTPID(String v) {
           this.tpid = v;
        }
         public void setRef(String v) {
           this.ref = v;
        }
          public void setRemarks(String v) {
           this.remarks = v;
        }
        public void setShipMethod(String v) {
           this.shipmethod = v;
        }
         public void setFODate(String v) {
           this.fodate = v;
        }
          public void setBOL(String v) {
           this.bol = v;
        }
          public void setWeight(String v) {
           this.weight = v;
        }  
       
 
        // header getters
        public String getCustFO() {
           return this.custfo;
        }
        public String getCarrier() {
           return this.carrier;
        }
        public String getEquipType() {
           return this.equiptype;
        }
        public String getCust() {
           return this.cust;
        }
        public String getTPID() {
           return this.tpid;
        }
        public String getRef() {
           return this.ref;
        }
          public String getRemarks() {
           return this.remarks;
        }
            public String getShipMethod() {
           return this.shipmethod;
        }
      
        public String getISASenderID() {
           return this.isaSenderID;
        }
        public String getISAReceiverID() {
           return this.isaReceiverID;
        }
        public String getDocType() {
           return this.doctype;
        }
        public String getDocID() {
           return this.docid;
        }
         public String getFODate() {
           return this.fodate;
        }
          public String getBOL() {
           return this.bol;
        }
        public String getWeight() {
           return this.weight;
        }
        

      
        
        
        public int getDetCount() {
            return detailArray.size();
        }
       
        
    }
     
     public static class edi990 {
    // Header fields
    public String isaSenderID = "";
    public String isaReceiverID = "";
    public String gsSenderID = "";
    public String gsReceiverID = "";
    public String isaCtrlNum = "";
    public String isaDate = "";
    public String doctype = "";
    public String docid = "";
    public String order = "";
    public String yesno = "";
    public String reasoncode = "";
    public String scac = "";
    
  
   
        
        public edi990() {
            
        }
        
        public edi990(String isasenderid, String isareceiverid, 
                      String gssenderid, String gsreceiverid,
                      String isactrlnum, String isadate, 
                      String doctype, String docid) {
            this.isaSenderID = isasenderid;
            this.isaReceiverID = isareceiverid;
            this.gsSenderID = gssenderid;
            this.gsReceiverID = gsreceiverid;
            this.isaCtrlNum = isactrlnum;
            this.isaDate = isadate;
            this.docid = docid;
            this.doctype = doctype;
        }
        
     
        
        
        
       // header setters 
        
        public void setOrder(String v) {
           this.order = v;
        }
        public void setYESNO(String v) {
           this.yesno = v;
        }
        public void setReasonCode(String v) {
           this.reasoncode = v;
        }
        public void setSCAC(String v) {
           this.scac = v;
        }
      
        
        // header getters
        public String getOrder() {
           return this.order;
        }
        public String getYESNO() {
           return this.yesno;
        }
        public String getReasonCode() {
           return this.reasoncode;
        }
        public String getSCAC() {
           return this.scac;
        }
       
        public String getISASenderID() {
           return this.isaSenderID;
        }
        public String getISAReceiverID() {
           return this.isaReceiverID;
        }
        public String getDocType() {
           return this.doctype;
        }
        public String getDocID() {
           return this.docid;
        }
     

        public String getIsaSenderID() {
            return isaSenderID;
        }

      
        
        
     
       
        
    }
     
     public static class edi997i {
    // Header fields
    public String isaSenderID = "";
    public String isaReceiverID = "";
    public String gsSenderID = "";
    public String gsReceiverID = "";
    public String isaCtrlNum = "";
    public String isaDate = "";
    public String doctype = "";
    public String gsCtrlNum = "";
    public String stCtrlNum = "";
    
  
    // Detail fields     
    public ArrayList<String[]> detailArray = new ArrayList<String[]>();
    public int DetFieldsCount997i = 1;
    public String[] initDetailArray(String[] a) {
        for (int i = 0; i < a.length; i++) {
            a[i] = "";
        }        
        return a;
    }
        
        public edi997i() {
            
        }
        
        public edi997i(String isasenderid, String isareceiverid, 
                      String gssenderid, String gsreceiverid,
                      String isactrlnum, String gsctrlnum, String isadate, 
                      String doctype) {
            this.isaSenderID = isasenderid;
            this.isaReceiverID = isareceiverid;
            this.gsSenderID = gssenderid;
            this.gsReceiverID = gsreceiverid;
            this.isaCtrlNum = isactrlnum;
            this.isaCtrlNum = gsctrlnum; 
            this.isaDate = isadate;
            this.doctype = doctype;
        }
        
      // detail setters
          public void setSTCtrlNum(int i, String v) {
          this.detailArray.get(i)[0] = v;
        }
      // detail getters 
           public String getSTCtrlNum(int i) {
            return detailArray.get(i)[0];
        }
        
     
        
       
      
        
        // header getters
        public String getISASenderID() {
           return this.isaSenderID;
        }
        public String getISAReceiverID() {
           return this.isaReceiverID;
        }
        public String getDocType() {
           return this.doctype;
        }
        public String getGSCtrlNum() {
           return this.gsCtrlNum;
        }
         public String getISACtrlNum() {
           return this.gsCtrlNum;
        }
       

         public int getDetCount() {
            return detailArray.size();
        }
        
        
     
       
        
    }
     
      public static class edi220 {
    // Header fields
    public String isaSenderID = "";
    public String isaReceiverID = "";
    public String gsSenderID = "";
    public String gsReceiverID = "";
    public String isaCtrlNum = "";
    public String isaDate = "";
    public String doctype = "";
    public String docid = "";
    public String order = "";
    public String yesno = "";
    public String remarks = "";
    public String scac = "";
    public String amount = "";
    
  
   
        
        public edi220() {
            
        }
        
        public edi220(String isasenderid, String isareceiverid, 
                      String gssenderid, String gsreceiverid,
                      String isactrlnum, String isadate, 
                      String doctype, String docid) {
            this.isaSenderID = isasenderid;
            this.isaReceiverID = isareceiverid;
            this.gsSenderID = gssenderid;
            this.gsReceiverID = gsreceiverid;
            this.isaCtrlNum = isactrlnum;
            this.isaDate = isadate;
            this.docid = docid;
            this.doctype = doctype;
        }
        
     
        
        
        
       // header setters 
        
        public void setOrder(String v) {
           this.order = v;
        }
        public void setAmount(String v) {
           this.amount = v;
        }
        public void setYESNO(String v) {
           this.yesno = v;
        }
        public void setRemarks(String v) {
           this.remarks = v;
        }
        public void setSCAC(String v) {
           this.scac = v;
        }
      
        
        // header getters
        public String getOrder() {
           return this.order;
        }
        public String getAmount() {
           return this.amount;
        }
        public String getYESNO() {
           return this.yesno;
        }
        public String getRemarks() {
           return this.remarks;
        }
        public String getSCAC() {
           return this.scac;
        }
       
        public String getISASenderID() {
           return this.isaSenderID;
        }
        public String getISAReceiverID() {
           return this.isaReceiverID;
        }
        public String getDocType() {
           return this.doctype;
        }
        public String getDocID() {
           return this.docid;
        }
     

        public String getIsaSenderID() {
            return isaSenderID;
        }

      
        
        
     
       
        
    }
     
      public static class edi214 {
    // Header fields
    public String isaSenderID = "";
    public String isaReceiverID = "";
    public String gsSenderID = "";
    public String gsReceiverID = "";
    public String isaCtrlNum = "";
    public String isaDate = "";
    public String doctype = "";
    public String docid = "";
    public String order = "";
    public String pronbr = "";
    public String status = "";
    public String remarks = "";
    public String scac = "";
    public String lat = "";
    public String lon = "";
    public String apptdate = "";
    public String appttime = "";
    public String equipmentnbr = "";
    public String equipmenttype = "";
    
  
   
        
        public edi214() {
            
        }
        
        public edi214(String isasenderid, String isareceiverid, 
                      String gssenderid, String gsreceiverid,
                      String isactrlnum, String isadate, 
                      String doctype, String docid) {
            this.isaSenderID = isasenderid;
            this.isaReceiverID = isareceiverid;
            this.gsSenderID = gssenderid;
            this.gsReceiverID = gsreceiverid;
            this.isaCtrlNum = isactrlnum;
            this.isaDate = isadate;
            this.docid = docid;
            this.doctype = doctype;
        }
        
     
        
        
        
       // header setters 
        
        public void setOrder(String v) {
           this.order = v;
        }
        public void setProNbr(String v) {
           this.pronbr = v;
        }
        public void setStatus(String v) {
           this.status = v;
        }
        public void setLat(String v) {
           this.lat = v;
        }
        public void setLon(String v) {
           this.lon = v;
        }
        public void setEquipmentNbr(String v) {
           this.equipmentnbr = v;
        }
        public void setEquipmentType(String v) {
           this.equipmenttype = v;
        }
        public void setApptDate(String v) {
           this.apptdate = v;
        }
        public void setApptTime(String v) {
           this.appttime = v;
        }
        public void setRemarks(String v) {
           this.remarks = v;
        }
        public void setSCAC(String v) {
           this.scac = v;
        }
      
        
        // header getters
        public String getOrder() {
           return this.order;
        }
        public String getProNbr() {
           return this.pronbr;
        }
        public String getStatus() {
           return this.status;
        }
        public String getRemarks() {
           return this.remarks;
        }
        public String getSCAC() {
           return this.scac;
        }
         public String getLat() {
           return this.lat;
        }
        public String getLon() {
           return this.lon;
        }
        public String getEquipmentType() {
           return this.equipmenttype;
        }
        public String getEquipmentNbr() {
           return this.equipmentnbr;
        }
        public String getApptDate() {
           return this.apptdate;
        }
        public String getApptTime() {
           return this.appttime;
        }
        public String getISASenderID() {
           return this.isaSenderID;
        }
        public String getISAReceiverID() {
           return this.isaReceiverID;
        }
        public String getDocType() {
           return this.doctype;
        }
        public String getDocID() {
           return this.docid;
        }
     

        public String getIsaSenderID() {
            return isaSenderID;
        }

      
        
        
     
       
        
    } 
      
     public static class edi862 {
    
    public String isaSenderID = "";
    public String isaReceiverID = "";
    public String gsSenderID = "";
    public String gsReceiverID = "";
    public String isaCtrlNum = "";
    public String isaDate = "";
    public String doctype = "";
    public String docid = "";
    public String po = "";
    public String rlse = "";
    public String billto = "";
    public String shipto = "";
    public String ov_billto = "";
    public String ov_shipto = "";
    public String remarks = "";
    public String shipmethod = "";
    public String duedate = "";
    public String podate = "";
    public String st_name = "";
    public String st_line1 = "";
    public String st_line2 = "";
    public String st_line3 = "";
    public String st_city = "";
    public String st_state = "";
    public String st_zip = "";
    public String st_country = "";
    
       
    public ArrayList<String> detsku = new ArrayList<String>();
    public ArrayList<String> detcustitem = new ArrayList<String>();
    public ArrayList<String> detitem = new ArrayList<String>();
    public ArrayList<String> detqty = new ArrayList<String>();
    public ArrayList<String> detref = new ArrayList<String>();
    public ArrayList<String> detpo = new ArrayList<String>();
    public ArrayList<String> detline = new ArrayList<String>();
    public ArrayList<String> detlistprice = new ArrayList<String>();
    public ArrayList<String> detnetprice = new ArrayList<String>();
    public ArrayList<String> detdisc = new ArrayList<String>();
    public ArrayList<String> dettype = new ArrayList<String>();
    public ArrayList<String> detdate = new ArrayList<String>();
    
    
    public ArrayList<String> FSTloopqty = new ArrayList<String>();
    public ArrayList<String> FSTloopdate = new ArrayList<String>();
    public ArrayList<String> FSTlooptype = new ArrayList<String>();
    public ArrayList<String> FSTloopref = new ArrayList<String>();
    
    public Map<Integer, ArrayList<String>> map = new HashMap<Integer, ArrayList<String>>();
    
    
    
        public edi862() {
            
        }
        public edi862(String isasenderid, String isareceiverid, 
                      String gssenderid, String gsreceiverid,
                      String isactrlnum, String isadate, 
                      String doctype, String docid) {
            this.isaSenderID = isasenderid;
            this.isaReceiverID = isareceiverid;
            this.gsSenderID = gssenderid;
            this.gsReceiverID = gsreceiverid;
            this.isaCtrlNum = isactrlnum;
            this.isaDate = isadate;
            this.docid = docid;
            this.doctype = doctype;
        }
       
        public void setFSTMap(Integer i, ArrayList<String> a) {
            this.map.put(i, a);
        }
        
        public void setFSTLoopDate(String v) {
           this.FSTloopdate.add(v);
        }
        public void setFSTLoopQty(String v)  {
           this.FSTloopqty.add(v);
        }
        public void setFSTLoopType(String v) {
           this.FSTlooptype.add(v);
        }
        public void setFSTLoopRef(String v) {
           this.FSTloopref.add(v);
        }
        
        public void setDetDate(String v) {
           this.detdate.add(v);
        }
        public void setDetType(String v) {
           this.dettype.add(v);
        }
        public void setDetItem(String v) {
           this.detitem.add(v);
        }
        public void setDetCustItem(String v) {
           this.detcustitem.add(v);
        }
        public void setDetSku(String v) {
           this.detsku.add(v);
        }
        public void setDetRef(String v) {
           this.detref.add(v);
        }
        public void setDetPO(String v) {
           this.detpo.add(v);
        }
        public void setDetLine(String v) {
           this.detline.add(v);
        }
        public void setDetQty(String v) {
           this.detqty.add(v);
        }
        public void setDetListPrice(String v) {
           this.detlistprice.add(v);
        }
         public void setDetNetPrice(String v) {
           this.detnetprice.add(v);
        }
          public void setDetDisc(String v) {
           this.detdisc.add(v);
        }
       
        public void setRlse(String v) {
           this.rlse = v;
        }  
        public void setPO(String v) {
           this.po = v;
        }
        public void setPODate(String v) {
           this.podate = v;
        }
         public void setDueDate(String v) {
           this.duedate = v;
        }
        public void setShipTo(String v) {
           this.shipto = v;
        }
        public void setShipToName(String v) {
           this.st_name = v;
        }
        public void setShipToLine1(String v) {
           this.st_line1 = v;
        }
        public void setShipToLine2(String v) {
           this.st_line2 = v;
        }
        public void setShipToLine3(String v) {
           this.st_line3 = v;
        }
        public void setShipToCity(String v) {
           this.st_city = v;
        }
        public void setShipToState(String v) {
           this.st_state = v;
        }
        public void setShipToZip(String v) {
           this.st_zip = v;
        }
        public void setShipToCountry(String v) {
           this.st_country = v;
        }
        public void setBillTo(String v) {
           this.billto = v;
        }
        public void setOVShipTo(String v) {
            this.ov_shipto = v;
        }
        public void setOVBillTo(String v) {
            this.ov_billto = v;
        }
        public String getPO() {
           return this.po;
        }
        public Map getFSAMap(int i) {
           return this.map;
        }
        public String getRlse() {
           return this.rlse;
        }
        public String getPODate() {
           return this.podate;
        }
        public String getShipTo() {
           return this.shipto;
        }
        public String getShipToName() {
           return this.st_name;
        }
        public String getShipToLine1() {
           return this.st_line1;
        }
        public String getShipToLine2() {
           return this.st_line2;
        }
        public String getShipToLine3() {
           return this.st_line3;
        }
        public String getShipToCity() {
           return this.st_city;
        }
        public String getShipToState() {
           return this.st_state;
        }
        public String getShipToZip() {
           return this.st_zip;
        }
        public String getShipToCountry() {
           return this.st_country;
        }
        public String getBillTo() {
           return this.billto;
        }
        public String getOVShipTo() {
           return this.ov_shipto;
        }
        public String getOVBillTo() {
           return this.ov_billto;
        }
        public String getISASenderID() {
           return this.isaSenderID;
        }
        public String getISAReceiverID() {
           return this.isaReceiverID;
        }
        public String getDocType() {
           return this.doctype;
        }
        public String getDocID() {
           return this.docid;
        }

        public String getIsaSenderID() {
            return isaSenderID;
        }

        public String getFSTLoopDate(int i) {
            return FSTloopdate.get(i);
        }
        public String getFSTLoopQty(int i) {
            return FSTloopqty.get(i);
        }
        public String getFSTLoopType(int i) {
            return FSTlooptype.get(i);
        }
         public String getFSTLoopRef(int i) {
            return FSTloopref.get(i);
        }
        
        public String getDetDate(int i) {
            return detdate.get(i);
        }
        public String getDetType(int i) {
            return dettype.get(i);
        }
        public String getDetItem(int i) {
            return detitem.get(i);
        }
        public String getDetCustItem(int i) {
           return detcustitem.get(i);
        }
        public String getDetSku(int i) {
          return detsku.get(i);
        }
        public String getDetRef(int i) {
           return detref.get(i);
        }
        public String getDetPO(int i) {
           return detpo.get(i);
        }
        public String getDetLine(int i) {
           return detline.get(i);
        }
        public String getDetQty(int i) {
           return detqty.get(i);
        }
        public String getDetListPrice(int i) {
           return detlistprice.get(i);
        }
        public String getDetNetPrice(int i) {
           return detnetprice.get(i);
        }
        public String getDetDisc(int i) {
           return detdisc.get(i);
        }
                
        public int getDetCount() {
            return detitem.size();
        }
       
        public int getFCSCount() {
            return FSTloopdate.size();
        }
        
    }
    
}
