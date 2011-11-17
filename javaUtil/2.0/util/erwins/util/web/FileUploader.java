
package erwins.util.web;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import erwins.util.exception.ExceptionUtil;
import erwins.util.lib.FileUtil;

/**
 * common을 이용한 파일 업로드.
 * Apache Commons를 이용, req에서 파일을 추출한다.
 * ProgressListener는 사용하지 않는다.
 * 추후 command를 넣어서 밸리데이션 체크를 하자.
 * 인코딩은 브라우저 jsp설정과 연관되는듯 하다.
 */
public class FileUploader{
	
    private File repositoryPath;
    private FileFilter filter;
    private String encoding = "UTF-8";
    private int maxMb = 1024*2;
    
    public FileUploader(File repositoryPath){
    	this.repositoryPath = repositoryPath;
    }
    
    private void validate(HttpServletRequest req){
        boolean isMultipart = ServletFileUpload.isMultipartContent(req);
        if(!isMultipart) throw new IllegalArgumentException("request is not multi/part");
    }

    /** 리턴되는 map값에는 폼파라메터 값이 들어간다.
     * upload되는 파일은 유일이름으로 변경되어 오버라이딩 되지 않는다.
     *  */
    public UploadResult upload(HttpServletRequest req,FileUploadRename fileUploadRename){
    	validate(req);
    	if(fileUploadRename==null) fileUploadRename = RENAME_DEFAULT ;
    	
    	UploadResult result = new UploadResult();
    	
        int yourMaxMemorySize = 1024 * 200;                 // threshold  값 설정 (0.2M?) 초기값은 0.01메가
        long yourMaxRequestSize = 1024 * 1024 * maxMb;   
        
        DiskFileItemFactory factory = new DiskFileItemFactory();
        factory.setSizeThreshold(yourMaxMemorySize);
        factory.setRepository(repositoryPath);
        
        ServletFileUpload upload = new ServletFileUpload(factory);
        //upload.setHeaderEncoding("EUC-KR"); //수정!!
        upload.setHeaderEncoding(encoding);
        upload.setSizeMax(yourMaxRequestSize);  // 임시 업로드 디렉토리 설정
        //upload.setProgressListener(listener);  //리스너는 사용하지 않는다.
        
        FileItem item = null;
        List<?> items;
        try {
            items = upload.parseRequest(req);
            for(Object aItem : items) {
                item = (FileItem)aItem;
                if(item.isFormField()) {
                	result.parameter.put(item.getFieldName(), item.getString(encoding));
                }else{
                    String fileName = item.getName();
                    fileName = fileName.substring(fileName.lastIndexOf(File.separator) + 1); //다시보기
                    File uploadedFile = new File(repositoryPath,fileName);
                    if(filter != null && !filter.isStorable(uploadedFile)) continue;
                    
                    File renamed = fileUploadRename.nameTo(uploadedFile); //OS가 한글을 인식 못할 수 있음으로 실제 쓰기전 rename한다.
                    item.write(renamed);
                    result.uploadedFiles.add(new UploadedFile(renamed,fileName));
                    //fileItem.get();  //메모리에 모두 할당
                }
            }
        } catch (Exception e) {
            ExceptionUtil.castToRuntimeException(e);
        }
        return result;
    }
    
    public static class UploadResult{
    	public final Map<String,String> parameter = new HashMap<String,String>();
    	public final List<UploadedFile> uploadedFiles = new ArrayList<UploadedFile>();
    }
    
    public static class UploadedFile{
    	public final File file;
    	public final String orgName;
    	public UploadedFile(File file,String orgName){
    		this.file = file;
    		this.orgName = orgName;
    	}
    }
    
    private static final FileUploadRename RENAME_DEFAULT = new FileUploadRename(){
		@Override
		public File nameTo(File uploadedFile) {
			return FileUtil.uniqueFileName(uploadedFile);
		}
    };
    
    /** 디폴트 값은 UTF-8 */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /** 디폴트 값은 1024*2 인 2기가. */
    public void setMaxMb(int maxMb) {
        this.maxMb = maxMb;
    }

    /** ex)  up.setFilter(new FileFilter(){
            public boolean isStorable(File file) {
                log.debug(file.getAbsolutePath()+" is uploaded.");
                return true;
            }
        }); */
    public void setFilter(FileFilter filter) {
        this.filter = filter;
    }

    public static interface FileFilter{
        /** false를 리턴하면 저장하지 않는다. */
        public boolean isStorable(File file);
    }
    
    public static interface FileUploadRename{
    	public File nameTo(File uploadedFile);
    }

}
