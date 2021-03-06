package mslinks;

import mslinks.io.ByteReader;
import mslinks.io.ByteWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;

import mslinks.data.ItemID;

public class LinkTargetIDList extends LinkedList<ItemID> implements Serializable {
	
	public LinkTargetIDList() {}
	
	public LinkTargetIDList(ByteReader data) throws IOException, ShellLinkException {
		int size = (int)data.read2bytes();
		
		int pos = data.getPosition(); 
		
		boolean binary = false;
		int s = (int)data.read2bytes();
		while (s != 0) {
			s -= 2;
			if (binary) {
				byte[] b = new byte[s];
				for (int i=0; i<s; i++)
					b[i] = (byte)data.read();
				add(new ItemID(b));
			} else try {
				add(new ItemID(data));
			} catch (UnsupportedCLSIDException e) {
				System.err.println("unsupported CLSID");
				binary = true;
			}
			s = (int)data.read2bytes();
		}
		
		pos = data.getPosition() - pos;
		if (pos != size) 
			throw new ShellLinkException();
	}

	public void serialize(ByteWriter bw) throws IOException {
		int size = 2;
		byte[][] b = new byte[size()][];
		int i = 0;
		for (ItemID j : this) {
			ByteArrayOutputStream ba = new ByteArrayOutputStream();
			ByteWriter w = new ByteWriter(ba);
			
			j.serialize(w);			
			b[i++] = ba.toByteArray();					
		}			
		for (byte[] j : b)
			size += j.length + 2;
		
		bw.write2bytes(size);
		for (byte[] j : b) {
			bw.write2bytes(j.length + 2);
			bw.writeBytes(j);
		}
		bw.write2bytes(0);
	}
	
	public boolean isCorrect() {
		for (ItemID i : this) 
			if (i.getType() == ItemID.TYPE_UNKNOWN)
				return false;
		return true;
	}
}
