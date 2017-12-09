import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SynacorVM {

	public static final int INT_MAX = 32768;
	public static final int REGISTERS = 8;
	public static final int STACK_POS = INT_MAX + REGISTERS;
	public static final int STACK_SIZE = INT_MAX * 2 - STACK_POS;

	public static final int HALT = 0;
	public static final int SET = 1;
	public static final int PUSH = 2;
	public static final int POP = 3;
	public static final int EQ = 4;
	public static final int GT = 5;
	public static final int JMP = 6;
	public static final int JT = 7;
	public static final int JF = 8;
	public static final int ADD = 9;
	public static final int MULT = 10;
	public static final int MOD = 11;
	public static final int AND = 12;
	public static final int OR = 13;
	public static final int NOT = 14;
	public static final int RMEM = 15;
	public static final int WMEM = 16;
	public static final int CALL = 17;
	public static final int RET = 18;
	public static final int OUT = 19;
	public static final int IN = 20;
	public static final int NOOP = 21;

	private static int val(int[] mem, int p) {
		return mem[p] < INT_MAX ? mem[p] : mem[mem[p]];
	}

	private int[] mem = new int[INT_MAX + REGISTERS + STACK_SIZE + 2];
	private int p = 0;
	private int s = 0;

	public void load(String filename) throws IOException {
		byte[] bytes = Files.readAllBytes(Paths.get(filename));
		for (int i = 0; i < mem.length; i++) {
			if (i < bytes.length / 2) {
				mem[i] = ((bytes[2 * i + 1] & 0x7f) << 8) | (bytes[2 * i] & 0xff);
			} else {
				mem[i] = 0;
			}
		}
		p = mem[mem.length - 1];
		s = mem[mem.length - 2];
	}

	public void save(String filename) throws IOException {
		byte[] bytes = new byte[mem.length * 2];
		mem[mem.length - 1] = p;
		mem[mem.length - 2] = s;
		for (int i = 0; i < mem.length; i++) {
			bytes[i * 2] = (byte) (mem[i] & 0xff);
			bytes[i * 2 + 1] = (byte) ((mem[i] & 0x7f00) >> 8);
		}
		Files.write(Paths.get(filename), bytes);
	}

	public static void main(String[] args) throws IOException {
		SynacorVM vm = new SynacorVM();
		if (Paths.get("savegame.bin").toFile().exists()) {
			vm.load("savegame.bin");
		} else {
			vm.load("challenge.bin");
		}
		vm.run();
	}

	public boolean run() throws IOException {
		while (true) {
			switch (mem[p]) {
			case HALT:
				return true;
			case SET:
				mem[mem[p + 1]] = val(mem, p + 2);
				p += 3;
				break;
			case PUSH:
				mem[STACK_POS + s] = val(mem, p + 1);
				s++;
				p += 2;
				break;
			case POP:
				if (s == 0) {
					return false;
				}
				s--;
				mem[mem[p + 1]] = mem[STACK_POS + s];
				p += 2;
				break;
			case EQ:
				mem[mem[p + 1]] = (val(mem, p + 2) == val(mem, p + 3)) ? 1 : 0;
				p += 4;
				break;
			case GT:
				mem[mem[p + 1]] = (val(mem, p + 2) > val(mem, p + 3)) ? 1 : 0;
				p += 4;
				break;
			case JMP:
				p = mem[p + 1];
				break;
			case JT:
				if (val(mem, p + 1) != 0) {
					p = val(mem, p + 2);
				} else {
					p += 3;
				}
				break;
			case JF:
				if (val(mem, p + 1) == 0) {
					p = val(mem, p + 2);
				} else {
					p += 3;
				}
				break;
			case ADD:
				mem[mem[p + 1]] = (val(mem, p + 2) + val(mem, p + 3)) % INT_MAX;
				p += 4;
				break;
			case MULT:
				mem[mem[p + 1]] = (val(mem, p + 2) * val(mem, p + 3)) % INT_MAX;
				p += 4;
				break;
			case MOD:
				mem[mem[p + 1]] = val(mem, p + 2) % val(mem, p + 3);
				p += 4;
				break;
			case AND:
				mem[mem[p + 1]] = val(mem, p + 2) & val(mem, p + 3);
				p += 4;
				break;
			case OR:
				mem[mem[p + 1]] = val(mem, p + 2) | val(mem, p + 3);
				p += 4;
				break;
			case NOT:
				mem[mem[p + 1]] = (~val(mem, p + 2)) & 0x7fff;
				p += 3;
				break;
			case RMEM:
				mem[mem[p + 1]] = mem[val(mem, p + 2)];
				p += 3;
				break;
			case WMEM:
				mem[val(mem, p + 1)] = val(mem, p + 2);
				p += 3;
				break;
			case CALL:
				mem[STACK_POS + s] = p + 2;
				s++;
				p = val(mem, p + 1);
				break;
			case RET:
				if (s == 0) {
					return false;
				}
				s--;
				p = mem[STACK_POS + s];
				break;
			case OUT:
				System.out.write(val(mem, p + 1));
				System.out.flush();
				p += 2;
				break;
			case IN:
				int ch = System.in.read();
				if (ch == 16) /* ^P */ {
					save("savegame.bin");
				}
				mem[mem[p + 1]] = ch;
				p += 2;
				break;
			case NOOP:
				p += 1;
				break;
			}
		}
	}

}
