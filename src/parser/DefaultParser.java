package parser;

import data.Data;
import data.DefaultData;

public class DefaultParser implements Parser{

	@Override
	public Data parse(String content) {
		return new DefaultData(content);
	}

}
