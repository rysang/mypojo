package erwins.util.spring.batch;


import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.ResourceAwareItemWriterItemStream;
import org.springframework.core.io.Resource;

import au.com.bytecode.opencsv.CSVParser;
import au.com.bytecode.opencsv.CSVWriter;
import erwins.util.root.NotThreadSafe;
import erwins.util.vender.etc.OpenCsv;

/** 
 * CsvItemReader 와는 다르게 스레드 세이프해질 수 없다~
 */
@NotThreadSafe
public class CsvItemWriter<T> implements ResourceAwareItemWriterItemStream<T>,ItemWriter<T>, ItemStream {
	
	public static final String WRITE_COUNT = "write.count";
	
	private CSVWriter writer;
	private Resource resource;
	private String encoding = "MS949";
	/** 수동 헤더 */
	private String[] header;
	/** SQL등으로 생성되는 헤더 */
	private CsvHeaderCallback csvHeaderCallback;
	private CsvAggregator<T> csvAggregator;
	private int lineCount = 0;
	private int bufferSize = 1024*1024; //1메가?
	private boolean first = true;
	/** true이면 뒤에 붙여쓴다 */
	private boolean append = false;
	
	public static interface CsvHeaderCallback{
		public List<String[]> headers();
	}
	
	/** CSV로 write한것을 다시 CSV로 읽으려면 동일한 이스케이퍼(\)를 사용해야 한다.
	 * 대신 이렇게 이스케이핑 하면 MS의 엑셀 프로그램으로 읽지 못한다.(엑셀의 경우 기본 이스케이퍼(")를 사용한다.)  */
	private boolean csvRead = false;
	
	/** 확실히 버퍼는 작동하는듯 하다. F5 연타하면 깔끔하게 1메가씩 올라간다. 
	 * 근데 성능 차이는 없는거 같다.. (확인은 안해봄) */
	@Override
	public void open(ExecutionContext executionContext) throws ItemStreamException {
		try {
			FileOutputStream os = new FileOutputStream(resource.getFile(),append);
			OutputStreamWriter w = new OutputStreamWriter(os,encoding);
			BufferedWriter ww = new BufferedWriter(w,bufferSize); //디폴트가 8192 일듯
			char escaper = csvRead ? CSVParser.DEFAULT_ESCAPE_CHARACTER : CSVWriter.DEFAULT_ESCAPE_CHARACTER;
			writer = new CSVWriter(ww,CSVWriter.DEFAULT_SEPARATOR,CSVWriter.DEFAULT_QUOTE_CHARACTER,escaper);
			if(header!=null) writeLine(header);
		} catch (IOException e) {
			throw new ItemStreamException(e);
		}
	}
	
	@Override
	public void update(ExecutionContext executionContext) throws ItemStreamException {
		executionContext.putInt(WRITE_COUNT, lineCount);
	}
	
	@Override
	public void close() throws ItemStreamException {
		OpenCsv.closeQuietly(writer);
	}
	
	/** SQL의 메타데이터는 read()후에 계산됨으로 header에 쓰는 부분을 여기에 둔다 */
	@Override
	public void write(List<? extends T> items) throws Exception {
		if(first && csvHeaderCallback!=null){
			List<String[]> headers = csvHeaderCallback.headers(); 
			for(String[] each : headers) writeLine(each);
			first = false;
		}
		for(T item : items){
			String[] lines =  csvAggregator.aggregate(item);
			writeLine(lines);
		}
	}
	
	public void writeLine(String[] lines){
		lineCount++;
		writer.writeNext(lines);
	}
	
	@Override
	public void setResource(Resource resource) {
		this.resource = resource;
	}

	public void setHeader(String[] header) {
		this.header = header;
	}
	
	public void setCsvAggregator(CsvAggregator<T> csvAggregator) {
		this.csvAggregator = csvAggregator;
	}
	
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}
	
	public int getBufferSize() {
		return bufferSize;
	}

	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	public static interface CsvAggregator<T> {
		public String[] aggregate(T item);
	}
	
	/** 걍 내려준다.  */
	public static class PassThroughCsvAggregator implements CsvAggregator<String[]>{
		@Override
		public String[] aggregate(String[] item) {
			return item;
		}
	}
	
	public CsvHeaderCallback getCsvHeaderCallback() {
		return csvHeaderCallback;
	}

	public void setCsvHeaderCallback(CsvHeaderCallback csvHeaderCallback) {
		this.csvHeaderCallback = csvHeaderCallback;
	}

	public void setAppend(boolean append) {
		this.append = append;
	}

	public void setCsvRead(boolean csvRead) {
		this.csvRead = csvRead;
	}
	
	

}