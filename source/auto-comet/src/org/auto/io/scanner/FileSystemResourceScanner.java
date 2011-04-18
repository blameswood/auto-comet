package org.auto.io.scanner;

import java.io.File;

import org.auto.io.FileResource;
import org.auto.io.Resource;

/**
 * 资源扫描器
 *
 * @author XiaohangHu
 * */
public class FileSystemResourceScanner extends AbstractPatternResourceScanner {

	private DefaultFilePatternScanner fileScanner;

	public FileSystemResourceScanner(String locationPattern) {

		DefaultFilePatternScanner scanner = new DefaultFilePatternScanner();
		scanner.addHandler(new FileHandler() {

			@Override
			public void handle(File file) {
				Resource resource = new FileResource(file);
				handleResource(resource);
			}

		});
	}

	@Override
	public void scan(String locationPattern) {
		fileScanner.scan(locationPattern);
	}

}
