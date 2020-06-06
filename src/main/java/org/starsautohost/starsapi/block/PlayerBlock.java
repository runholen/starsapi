package org.starsautohost.starsapi.block;

import org.starsautohost.starsapi.Util;

public class PlayerBlock extends Block {
	
	public static boolean ignoreParseErrors = false;
	
    public static class PRT {
        public static int HE = 0;
        public static int SS = 1;
        public static int WM = 2;
        public static int CA = 3;
        public static int IS = 4;
        public static int SD = 5;
        public static int PP = 6;
        public static int IT = 7;
        public static int AR = 8;
        public static int JOAT = 9;
    }
    
    public int playerNumber; 
    public int shipDesignCount;
    public int planets;
    public int fleets;
    public int starbaseDesignCount;
    public int logo;
    public boolean fullDataFlag;
    public byte[] fullDataBytes;
    public byte[] playerRelations = new byte[0]; // 0 neutral, 1 friend, 2 enemy
    public byte[] nameBytes;
    public byte byte7 = 1;
    
	public PlayerBlock() {
		typeId = BlockType.PLAYER;
	}

	@Override
	public void decode() throws Exception {
	    playerNumber = decryptedData[0] & 0xFF;
	    shipDesignCount = decryptedData[1] & 0xFF;
	    planets = (decryptedData[2] & 0xFF) + ((decryptedData[3] & 0x03) << 8);
	    if ((decryptedData[3] & 0xFC) != 0) {
	        throw new Exception("Unexpected player values " + this);
	    }
	    fleets = (decryptedData[4] & 0xFF) + ((decryptedData[5] & 0x03) << 8);
	    starbaseDesignCount = ((decryptedData[5] & 0xF0) >> 4);
        if ((decryptedData[5] & 0x0C) != 0) {
            throw new Exception("Unexpected player values " + this);
        }
	    logo = ((decryptedData[6] & 0xFF) >> 3);
	    fullDataFlag = (decryptedData[6] & 0x04) != 0;
        if ((decryptedData[6] & 0x03) != 3) {
            throw new Exception("Unexpected player values " + this);
        }
        byte7 = decryptedData[7];
        // TODO maybe AI doesn't have this?
//        if (decryptedData[7] != 1) {
//            throw new Exception("Unexpected player values " + this);
//        }
        int index = 8;
	    if (fullDataFlag) {
	        fullDataBytes = new byte[0x68];
	        System.arraycopy(decryptedData, 8, fullDataBytes, 0, 0x68);
	        index = 0x70;
	        int playerRelationsLength = decryptedData[index] & 0xFF;
	        playerRelations = new byte[playerRelationsLength];
            System.arraycopy(decryptedData, index + 1, playerRelations, 0, playerRelationsLength);
            index += 1 + playerRelationsLength;
	    }
	    int namesStart = index;
	    int singularNameLength = decryptedData[index++] & 0xFF;
	    index += singularNameLength;
	    int pluralNameLength = decryptedData[index++] & 0xFF;
	    index += pluralNameLength;
	    if (pluralNameLength == 0) index++; //Fix for empty plural name.
	    nameBytes = new byte[index - namesStart];
	    System.arraycopy(decryptedData, namesStart, nameBytes, 0, nameBytes.length);
	    if (index != size) {
	    	String error = "Unexpected player data size: " + this;
	    	error += "\nIndex: "+index+", size: "+size;
	    	error += "\nPlayer nr: "+playerNumber+", "+ Util.decodeStarsString(nameBytes);
	    	error += "\nBlock size: "+(decryptedData != null ? decryptedData.length : 0);
	    	if (ignoreParseErrors){
	    		System.out.println(error);
	    		System.out.println("Continuing anyway");
	    	}
	    	else throw new Exception(error);
	    }
	    //System.out.println("Index: "+index);
	}

	@Override
	public void encode() throws Exception {
	    if (fullDataFlag) {
            byte[] res = new byte[8 + 0x68 + 1 + playerRelations.length + nameBytes.length];
            res[0] = (byte)playerNumber;
            res[1] = (byte)shipDesignCount;
            res[2] = (byte)(planets & 0xFF);
            res[3] = (byte)(planets >> 8);
            res[4] = (byte)(fleets & 0xFF);
            res[5] = (byte)((starbaseDesignCount << 4) + (fleets >> 8));
            res[6] = (byte)((logo << 3) + 7);
            res[7] = byte7;
            System.arraycopy(fullDataBytes, 0, res, 8, fullDataBytes.length);
            res[0x70] = (byte)playerRelations.length;
            System.arraycopy(playerRelations, 0, res, 0x71, playerRelations.length);
            System.arraycopy(nameBytes, 0, res, 0x71 + playerRelations.length, nameBytes.length);
            setDecryptedData(res, res.length);
	    } else {
	        byte[] res = new byte[8 + nameBytes.length];
	        res[0] = (byte)playerNumber;
	        res[1] = (byte)shipDesignCount;
	        res[2] = (byte)(planets & 0xFF);
	        res[3] = (byte)(planets >> 8);
	        res[4] = (byte)(fleets & 0xFF);
	        res[5] = (byte)((starbaseDesignCount << 4) + (fleets >> 8));
	        res[6] = (byte)((logo << 3) + 3);
	        res[7] = byte7;
	        System.arraycopy(nameBytes, 0, res, 8, nameBytes.length);
	        setDecryptedData(res, res.length);
	    }
	}

	// CAs see this
	public boolean hasEnvironmentInfoOnly() {
	    if (!fullDataFlag) return false;
	    for (int i = 0; i < 8; i++) {
	        if (fullDataBytes[i] != 0) return false;
	    }
        for (int i = 17; i < 0x68; i++) {
            if (fullDataBytes[i] != 0) return false;
        }
        return true;
	}
	
	public byte getPlayerRelationsWith(int player) {
	    if (player >= playerRelations.length) {
	        return 0;
	    } else {
	        return playerRelations[player];
	    }
	}
	
	public void makeBeefyFullData(int prt, boolean nrse) {
        boolean hasEnvironmentInfo = hasEnvironmentInfoOnly();
	    fullDataFlag = true;
	    if (fullDataBytes == null) fullDataBytes = new byte[0x68];
	    if (!hasEnvironmentInfo) {
	        for (int i = 8; i < 17; i++) {
	            fullDataBytes[i] = (byte)0xFF; //tri-immune
	        }
	    }
        fullDataBytes[17] = 3; // growth rate, this is the point mine...
        for (int i = 18; i < 24; i++) {
            fullDataBytes[i] = 26; // tech
        }
        fullDataBytes[54] = 7; // pop efficiency
        if (prt == PRT.AR) {
            fullDataBytes[55] = 10;
            fullDataBytes[56] = 10;
            fullDataBytes[57] = 10;
            fullDataBytes[58] = 10;
            fullDataBytes[59] = 5;
            fullDataBytes[60] = 10;
        } else {
            fullDataBytes[55] = 15; // factories
            fullDataBytes[56] = 5;
            fullDataBytes[57] = 25;
            fullDataBytes[58] = 25; // mines
            fullDataBytes[59] = 2;
            fullDataBytes[60] = 25;
        }
        // #61 is "spend leftover points on"
        fullDataBytes[62] = 0; // expensive tech
        fullDataBytes[63] = 0;
        fullDataBytes[64] = 0;
        fullDataBytes[65] = 0;
        fullDataBytes[66] = 0;
        fullDataBytes[67] = 0; // end of tech
        fullDataBytes[68] = (byte)prt;
        // #69 is always 0...
        fullDataBytes[70] = nrse ? (byte)141 : 13; // lrts (IFE, ISB, ARM; make this 141 with NRSE)
        fullDataBytes[71] = 32; // lrts (RS)
        fullDataBytes[73] = (byte)128; // G-box checked; +32 is tech-starts-with-3
        fullDataBytes[74] = (byte)255; // all MT items
        fullDataBytes[75] = 15; // all MT items
	}

	public void setTech(int energy, int weapons, int propulsion, int construction, int electronics, int biotech) {
	    fullDataBytes[18] = (byte)energy;
        fullDataBytes[19] = (byte)weapons;
        fullDataBytes[20] = (byte)propulsion;
        fullDataBytes[21] = (byte)construction;
        fullDataBytes[22] = (byte)electronics;
        fullDataBytes[23] = (byte)biotech;
	}
	
	public void setMtMask(int mtMask) {
	    fullDataBytes[74] = (byte)(mtMask >> 8);
	    fullDataBytes[75] = (byte)(mtMask & 0xFF);
	}
	
	public static PlayerBlock createUnknownRacePlayerBlock(int playerNumber) {
	    // does it need a distinct logo? homeworld?
	    PlayerBlock block = new PlayerBlock();
	    block.playerNumber = playerNumber;
	    block.makeBeefyFullData(PRT.JOAT, false);
	    if (playerNumber < 9) {
	        block.nameBytes = new byte[] { 2, (byte)191, (byte)(203 + playerNumber), 2, (byte)191, (byte)(203 + playerNumber) };
	    } else {
	        // player number 9 is displayed as 10
            block.nameBytes = new byte[] { 3, (byte)191, (byte)203, (byte)(203 + playerNumber - 10), 3, (byte)191, (byte)203, (byte)(203 + playerNumber - 10) };
	    }
	    return block;
	}

	public byte[] getFullDataBytes() {
		return fullDataBytes;
	}

	public void setFullDataFlag(boolean b) {
		fullDataFlag = b;
	}
}
