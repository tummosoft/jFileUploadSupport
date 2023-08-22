package com.tummosoft;

import anywheresoftware.b4a.BA;
import anywheresoftware.b4a.BA.Events;
import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.core.ProgressListener;

import org.apache.commons.fileupload2.jakarta.JakartaServletFileUpload;
import org.apache.commons.fileupload2.jakarta.JakartaServletRequestContext;
import org.apache.commons.io.FileCleaningTracker;
//import org.apache.commons.io.FileUtils;

@BA.ShortName("jFileUploadSupport")
@BA.Version(1.01f)
@Events(values = {"UploadProgress (TotalKB as long, Percent as String)", "UploadCompleted (Success as boolean, FileName as String)"})
public class jFileUploadSupport {

    private String _ev;
    private File tempfile;
    private boolean _multifiles = false;
    private boolean _multipart = false;
    static private int _filesizemax = 3;
    static private int _totalsizemax = 10;
    private String _uploadpath = "";
   
    public void Initialize(String EventName) throws IOException {
        this._ev = EventName.toLowerCase(BA.cul);
    }

    public boolean getIsMultiFiles() {
        return _multifiles;
    }

    public boolean getIsMultipart() {
        return _multipart;
    }

    public void setFileSizeMax(int MB) {
        _filesizemax = MB;
    }

    public void setTotalSizeMax(int MB) {
        _totalsizemax = MB;
    }

    public void TempFile(String Dir, String FileName) {
        tempfile = new File(Dir, FileName);
    }

    public void UploadPath(String Dir) {
        _uploadpath = Dir;
    }

    public void MultipartHook(final BA ba, HttpServletRequest request) {

        JakartaServletRequestContext servletRequestContext = new JakartaServletRequestContext(request);
        boolean isMultipart = JakartaServletFileUpload.isMultipartContent(servletRequestContext);

        if (isMultipart) {
            _multipart = true;
            FileCleaningTracker tracker = new FileCleaningTracker();
            tracker.track(tempfile, tracker);

            DiskFileItemFactory fileitemfactory = DiskFileItemFactory.builder()
                    .setFileCleaningTracker(tracker)
                    .get();

            JakartaServletFileUpload servletFileUpload
                    = new JakartaServletFileUpload(fileitemfactory);
            servletFileUpload.setHeaderCharset(Charset.forName("utf-8"));
            servletFileUpload.setFileSizeMax(1024 * 1024 * _filesizemax);
            servletFileUpload.setSizeMax(1024 * 1024 * _totalsizemax);

            servletFileUpload.setProgressListener(new ProgressListener() {
                @Override
                public void update(long RreadSize, long AllSize, int i) {
                    BigDecimal bd = new BigDecimal(String.valueOf(RreadSize * 100 / AllSize));
                    bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
                    String percent = bd.toString();

                    ba.raiseEvent(jFileUploadSupport.this, _ev + "_uploadprogress", new Object[]{AllSize, percent});
                }
            });
            try {
                uploadParseRequest(ba, servletFileUpload, request, _uploadpath);
            } catch (IOException ex) {
                BA.LogError(ex.getMessage());
            }
        }
    }

    private String randomFileName() {
        String charcode = "qwertyuiopasdfghjklzxcvbnmn1234567890";
        int max = charcode.length() - 1;
        int min = 0;
        String newFileName = "";
        for (int j = 0; j < 3; j++) {
            Random random = new Random();
            int randomNumber = random.nextInt(max + 1 - min) + min;
            int randomnext = rnd(min, randomNumber);
            
            newFileName += newFileName + charcode.substring(randomnext, randomnext + 1);
        }

        return newFileName;
    }

    private int rnd(int min, int max) {
          Random random = new Random();
          int randomNumber = random.nextInt(max + 1 - min) + min;
          
          return randomNumber;
    }    
    
    
    private void uploadParseRequest(final BA ba, JakartaServletFileUpload upload, HttpServletRequest request, String uploadPath) throws IOException {
        try {
            
            List<FileItem> fileItems = upload.parseRequest(request);
            if (fileItems.size() > 1) {
                _multifiles = true;
            }
            String checkup = "";
            for (FileItem fileItem : fileItems) {
                if (fileItem.isFormField()) {
                    String fieldName = fileItem.getFieldName();
                    String value = fileItem.getString(Charset.forName("utf-8"));
                } else {
                    String uploadFileName = fileItem.getName();
                    if (uploadFileName.trim().equals("") || uploadFileName == null) {
                        continue;
                    }
                    String fileName = uploadFileName.substring(uploadFileName.lastIndexOf("/") + 1);
                    String fileExtName = uploadFileName.substring(uploadFileName.lastIndexOf(".") + 1);

                    String realPath = uploadPath;
                    File realPathFile = new File(realPath);
                    if (!realPathFile.exists()) {
                        realPathFile.mkdir();
                    }
                                       
                    InputStream inputStream = fileItem.getInputStream();
                    FileOutputStream fos = new FileOutputStream(realPath + "/" + fileName, false);
                    
                        byte[] buffer = new byte[1024 * 1024];
                        int len = 0;
                        while ((len = inputStream.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                        fos.close();
                        inputStream.close();
                        fileItem.delete();
                        
                        ba.raiseEvent(jFileUploadSupport.this, _ev + "_uploadcompleted", new Object[]{true, fileName});
                }

            }
        } catch (FileUploadException ex) {
            ba.raiseEvent(jFileUploadSupport.this, _ev + "_uploadcompleted", new Object[]{false, "", 0});
            BA.LogError("Upload Fail, cause: " + ex.getMessage());
        }
    }

}
