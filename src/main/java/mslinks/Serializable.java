package mslinks;

import java.io.IOException;

import mslinks.io.ByteWriter;

public interface Serializable {
	void serialize(ByteWriter bw) throws IOException;
}
