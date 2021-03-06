package org.starsautohost.starsapi.block;

import java.io.ByteArrayOutputStream;

import org.starsautohost.starsapi.Util;

public class PartialPlanetBlock extends Block {
    // Guesses about defense estimate to number of defenses mapping, based on which tech level defenses.
    private static int[] DEFENSES_ESTIMATES = 
//        {0, 13, 20, 28, 37, 47, 57, 69, 83, 98, 100, 100, 100, 100, 100, 100};
//        {0,  6, 10, 14, 18, 23, 28, 34, 41, 48, 57, 68, 83, 100, 100, 100}; 
        {0,  5,  8, 11, 15, 19, 23, 28, 34, 40, 48, 57, 69, 85, 100, 100}; 
//        {0,  4,  6,  9, 12, 15, 18, 22, 27, 32, 38, 45, 55, 68, 91, 100};
//        {0,  3,  5,  7,  9, 12, 14, 17, 21, 25, 30, 35, 43, 53, 71, 100};
    
    public int planetNumber;
    public int owner; // -1 for no owner
    public boolean isHomeworld;
    public boolean isInUseOrRobberBaron; // inhabited or remote mined???
    public boolean hasEnvironmentInfo;
    public boolean bitWhichIsOffForRemoteMiningAndRobberBaron;
    public boolean weirdBit; // I see this on or off somewhat randomly
    public boolean hasRoute;
    public boolean hasSurfaceMinerals;
    public boolean hasArtifact;
    public boolean hasInstallations;
    public boolean isTerraformed;
    public boolean hasStarbase;
    public byte[] fractionalMinConcBytes = { 0 };
    public int ironiumConc, boraniumConc, germaniumConc;
    public int gravity, temperature, radiation;
    public int origGravity, origTemperature, origRadiation;
    public int defensesEstimate; // in sixteenths of 100%
    public int popEstimate; // in 400s, up to 4090
    public long ironium, boranium, germanium, population;
    public int excessPop; // the difference between 0 and 100? got this name from other utilities
    public int mines;
    public int factories;
    public int defenses;
    public byte unknownInstallationsByte;
    public boolean contributeOnlyLeftoverResourcesToResearch;
    public boolean hasScanner;
    public int starbaseDesign;
    public byte[] starbaseBytes;
    public int routeShort;
    
    public int turn = -1;
    
	public PartialPlanetBlock() {
		typeId = BlockType.PARTIAL_PLANET;
	}

	@Override
	public void decode() throws Exception {
	    planetNumber = (decryptedData[0] & 0xFF) + ((decryptedData[1] & 7) << 8);
	    owner = (decryptedData[1] & 0xF8) >> 3;
	    if (owner == 31) owner = -1;
	    else if (owner >= 16) throw new Exception("Unexpected owner: " + this);
	    int flags = Util.read16(decryptedData, 2);
	    if ((flags & 0x0078) != 0) {
	        throw new Exception("Unexpected planet flags: " + this);
	    }
	    if ((flags & 0x0100) != 0x0100) {
	        throw new Exception("Unexpected planet flags: " + this);
	    }
	    isHomeworld = (flags & 0x80) != 0;
	    isInUseOrRobberBaron = (flags & 0x04) != 0;
	    hasEnvironmentInfo = (flags & 0x02) != 0;
	    bitWhichIsOffForRemoteMiningAndRobberBaron = (flags & 0x01) != 0;
	    weirdBit = (flags & 0x8000) != 0;
	    hasRoute = (flags & 0x4000) != 0;
	    hasSurfaceMinerals = (flags & 0x2000) != 0;
	    hasArtifact = (flags & 0x1000) != 0;
	    hasInstallations = (flags & 0x0800) != 0;
	    isTerraformed = (flags & 0x0400) != 0;
	    hasStarbase = (flags & 0x0200) != 0;
	    if (!bitWhichIsOffForRemoteMiningAndRobberBaron && !hasSurfaceMinerals && !isInUseOrRobberBaron) {
            throw new Exception("Unexpected planet flags for not data[2] & 1: " + this);
	    }
	    if (isInUseOrRobberBaron && typeId == BlockType.PARTIAL_PLANET && bitWhichIsOffForRemoteMiningAndRobberBaron) {
	        throw new Exception("Did not expect data[2] & 5 in partial planet: " + this);
	    }
        if (!isInUseOrRobberBaron && typeId == BlockType.PLANET) {
            throw new Exception("Expected data[2] & 4 in planet: " + this);
        }
        int index = 4;
        if (canSeeEnvironment()) {
            int preEnvironmentLengthByte = Util.read8(decryptedData[4]);
            if ((preEnvironmentLengthByte & 0xC0) != 0) {
                throw new Exception("Unexpected bits at data[3]: " + this);
            }
            int preEnvironmentLength = 1;
            preEnvironmentLength += (preEnvironmentLengthByte & 0x30) >> 4;
            preEnvironmentLength += (preEnvironmentLengthByte & 0x0C) >> 2;
            preEnvironmentLength += (preEnvironmentLengthByte & 0x03);
            fractionalMinConcBytes = new byte[preEnvironmentLength];
            System.arraycopy(decryptedData, 4, fractionalMinConcBytes, 0, preEnvironmentLength);
            index += preEnvironmentLength;
            ironiumConc = Util.read8(decryptedData[index++]);
            boraniumConc = Util.read8(decryptedData[index++]);
            germaniumConc = Util.read8(decryptedData[index++]);
            gravity = Util.read8(decryptedData[index++]);
            temperature = Util.read8(decryptedData[index++]);
            radiation = Util.read8(decryptedData[index++]);
            if (isTerraformed) {
                origGravity = Util.read8(decryptedData[index++]);
                origTemperature = Util.read8(decryptedData[index++]);
                origRadiation = Util.read8(decryptedData[index++]);
            }
            if (owner >= 0) {
                int estimatesShort = Util.read16(decryptedData, index);
                defensesEstimate = estimatesShort / 4096;
                popEstimate = estimatesShort % 4096;
                index += 2;
            }
        }
        if (hasSurfaceMinerals) {
            int contentsLengths = Util.read8(decryptedData[index]);
            int iLength = contentsLengths & 0x03;
            iLength = 4 >> (3 - iLength);
            int bLength = (contentsLengths & 0x0C) >> 2;
            bLength = 4 >> (3 - bLength);
            int gLength = (contentsLengths & 0x30) >> 4;
            gLength = 4 >> (3 - gLength);
            int popLength = (contentsLengths & 0xC0) >> 6;
            popLength = 4 >> (3 - popLength);
            index += 1;
            ironium = Util.readN(decryptedData, index, iLength);
            index += iLength;
            boranium = Util.readN(decryptedData, index, bLength);
            index += bLength;
            germanium = Util.readN(decryptedData, index, gLength);
            index += gLength;
            population = Util.readN(decryptedData, index, popLength);
            index += popLength;
        }
        if (hasInstallations) {
            byte[] installationsBytes = new byte[8];
            System.arraycopy(decryptedData, index, installationsBytes, 0, 8);
            index += 8;
            excessPop = installationsBytes[0] & 0xFF;
            mines = (installationsBytes[1] & 0xFF) | (installationsBytes[2] & 0x0F) << 8;
            factories = (installationsBytes[2] & 0xF0) >> 4 | (installationsBytes[3] & 0xFF) << 4;
            defenses = installationsBytes[4];
            unknownInstallationsByte = installationsBytes[5];
            contributeOnlyLeftoverResourcesToResearch = (installationsBytes[6] & 0x80) != 0;
            hasScanner = (installationsBytes[6] & 0x01) == 0;
            if ((installationsBytes[6] & 0x7E) != 0 || installationsBytes[7] != 0) {
                throw new Exception("Unexpected installations data: " + this);
            }
        }
        if (hasStarbase) {
            if (typeId == BlockType.PARTIAL_PLANET) {
                byte starbaseByte = decryptedData[index++];
                if ((starbaseByte & 0xF0) != 0) {
                    throw new Exception("Unexpected starbase byte: " + this);
                }
                starbaseDesign = starbaseByte;
            } else {
                starbaseBytes = new byte[4];
                System.arraycopy(decryptedData, index, starbaseBytes, 0, 4);
                index += 4;
                starbaseDesign = starbaseBytes[0] & 0x0F;
            }
        }
        if (hasRoute && typeId == BlockType.PLANET) {
            routeShort = Util.read16(decryptedData, index);
            index += 2;
        }
        if (index != size && index + 2 != size) {
            throw new Exception("Unexpected planet data: " + this);
        }
        if (index + 2 == size) {
            turn = Util.read16(decryptedData, index);
        }
	}

    public boolean canSeeEnvironment() {
        return hasEnvironmentInfo || ((hasSurfaceMinerals || isInUseOrRobberBaron) && !bitWhichIsOffForRemoteMiningAndRobberBaron);
    }

	
    public void convertToPartialPlanetForMFile() throws Exception {
        convertToPartialPlanetForHFile(-1);
    }
    
	public void convertToPartialPlanetForHFile(int turn) throws Exception {
	    this.typeId = BlockType.PARTIAL_PLANET;
	    if (canSeeEnvironment()) hasEnvironmentInfo = true;
	    isInUseOrRobberBaron = false;
	    bitWhichIsOffForRemoteMiningAndRobberBaron = true;
	    // hasRoute = false;
	    hasSurfaceMinerals = false;
	    hasArtifact = false;
	    hasInstallations = false;
	    if (turn != -1) this.turn = turn;
	    this.encode();
	}

    public void convertToPartialPlanetForMFileWithMinerals() throws Exception {
        this.typeId = BlockType.PARTIAL_PLANET;
        // hasRoute = false;
        hasArtifact = false;
        hasInstallations = false;
        this.turn = -1;
        if (isInUseOrRobberBaron || hasSurfaceMinerals) {
            isInUseOrRobberBaron = true;
            bitWhichIsOffForRemoteMiningAndRobberBaron = false;
            population = 0;
            // hasSurfaceMinerals = false;
        } else {
            isInUseOrRobberBaron = false;
            bitWhichIsOffForRemoteMiningAndRobberBaron = true;
            hasSurfaceMinerals = false;
        }
        this.encode();
    }
    
    public static PlanetBlock convertToPlanetBlockForHstFile(PartialPlanetBlock block) throws Exception {
        PlanetBlock res;
        if (block.typeId == BlockType.PLANET) {
            res = (PlanetBlock)block;
        } else {
            block.typeId = BlockType.PLANET;
            block.starbaseBytes = new byte[] { (byte)block.starbaseDesign, 0, 0, 0 };
            block.hasRoute = false;
            block.encode();
            res = new PlanetBlock();
            res.setDecryptedData(block.getDecryptedData(), block.size);
            res.getDecryptedData()[2] |= 4;
            res.decode();
        }
        // if we can see minerals and there are none, don't create them
        boolean couldSeeEnvironment = block.canSeeEnvironment();
        boolean hadMinerals = block.isInUseOrRobberBaron || block.hasSurfaceMinerals;
        boolean hadInstallations = block.hasInstallations;
        if (!hadMinerals && block.owner >= 0) {
            res.hasSurfaceMinerals = true;
        }
        if (!hadInstallations && block.owner >= 0) {
            if (couldSeeEnvironment && block.popEstimate == 0) {
                // it's AR; may have limited installations data, but may have none
            } else {
                // even if not AR a planet may have no installations, if it was
                // just colonized
                res.hasInstallations = true;
            }
        }
        res.isInUseOrRobberBaron = true;
        res.hasEnvironmentInfo = true;
        res.bitWhichIsOffForRemoteMiningAndRobberBaron = true;
        res.weirdBit = false; //?
        res.turn = -1;
        if (!couldSeeEnvironment) {
            res.fractionalMinConcBytes = new byte[] { 0 };
            res.ironiumConc = 50;
            res.boraniumConc = 50;
            res.germaniumConc = 50;
            res.gravity = 50;
            res.temperature = 50;
            res.radiation = 50;
            res.defensesEstimate = 15;
            res.popEstimate = 3300;
        }
        if (!hadMinerals && res.hasSurfaceMinerals) {
            res.ironium = 50000;
            res.boranium = 50000;
            res.germanium = 50000;
            if (res.population == 0 && res.owner >= 0) {
                if (couldSeeEnvironment && res.popEstimate > 0) {
                    res.population = 4 * res.popEstimate;
                } else if (couldSeeEnvironment && res.popEstimate == 0) {
                    // being lazy about checking the starbase hull
                    res.population = 30000;
                } else {
                    // being lazy about checking the prt of the owner
                    res.population = 13200;
                }
            }
        }
        if (!hadInstallations && res.hasInstallations) {
            res.mines = 3300;
            res.factories = 3300;
            if (couldSeeEnvironment) {
                res.defenses = DEFENSES_ESTIMATES[res.defensesEstimate];
            } else {
                res.defenses = 100;
            }
            res.hasScanner = true;
        }
        res.encode();
        return res;
    }
    
    public static PlanetBlock createEmptyPlanetForHstFile(int planetNumber) throws Exception {
        PlanetBlock res = new PlanetBlock();
        res.planetNumber = planetNumber;
        res.owner = -1;
        res.isInUseOrRobberBaron = true;
        res.hasEnvironmentInfo = true;
        res.bitWhichIsOffForRemoteMiningAndRobberBaron = true;
        res.ironiumConc = 100;
        res.boraniumConc = 100;
        res.germaniumConc = 100;
        res.gravity = 50;
        res.temperature = 50;
        res.radiation = 50;
        res.encode();
        return res;
    }

	@Override
	public void encode() throws Exception {
	    ByteArrayOutputStream bout = new ByteArrayOutputStream();
	    bout.write(planetNumber & 0xFF);
	    bout.write((planetNumber >> 8) + (owner << 3));
	    int flag1 = 0;
	    if (isHomeworld) flag1 = flag1 | 0x80;
	    if (isInUseOrRobberBaron) flag1 = flag1 | 0x04;
	    if (hasEnvironmentInfo) flag1 = flag1 | 0x02;
	    if (bitWhichIsOffForRemoteMiningAndRobberBaron) flag1 = flag1 | 0x01;
	    bout.write(flag1);
	    int flag2 = 1;
	    if (weirdBit) flag2 = flag2 | 0x80;
	    if (hasRoute) flag2 = flag2 | 0x40;
	    if (hasSurfaceMinerals) flag2 = flag2 | 0x20;
	    if (hasArtifact) flag2 = flag2 | 0x10;
	    if (hasInstallations) flag2 = flag2 | 0x08;
	    if (isTerraformed) flag2 = flag2 | 0x04;
	    if (hasStarbase) flag2 = flag2 | 0x02;
	    bout.write(flag2);
        if (canSeeEnvironment()) {
	        if (fractionalMinConcBytes != null) bout.write(fractionalMinConcBytes); 
	        bout.write(ironiumConc);
	        bout.write(boraniumConc);
	        bout.write(germaniumConc);
	        bout.write(gravity);
	        bout.write(temperature);
	        bout.write(radiation);
            if (isTerraformed) {
                bout.write(origGravity);
                bout.write(origTemperature);
                bout.write(origRadiation);
            }
            if (owner >= 0) {
                bout.write(popEstimate & 0xFF);
                bout.write((popEstimate >> 8) | (defensesEstimate << 4));
            }
        }
        if (hasSurfaceMinerals) {
            byte[] res = new byte[getContentLength()];
            int index = 1;
            int iLength = Util.writeN(res, index, ironium);
            index += iLength;
            if (iLength == 4) iLength = 3;
            int bLength = Util.writeN(res, index, boranium);
            index += bLength;
            if (bLength == 4) bLength = 3;
            int gLength = Util.writeN(res, index, germanium);
            index += gLength;
            if (gLength == 4) gLength = 3;
            int popLength = Util.writeN(res, index, population);
            index += popLength;
            if (popLength == 4) popLength = 3;
            byte igbpopByte = (byte)(iLength | (bLength << 2) | (gLength << 4) | (popLength << 6));
            res[0] = igbpopByte;
            bout.write(res);
        }
        if (hasInstallations) {
            bout.write(excessPop);
            bout.write(mines & 0xFF);
            bout.write((factories & 0x0F) << 4 | (mines & 0x0F00) >>8);
            bout.write((factories & 0x0FF0) >> 4);
            bout.write(defenses);
            bout.write(unknownInstallationsByte);
            bout.write((contributeOnlyLeftoverResourcesToResearch ? 0x80 : 0) | (hasScanner ? 0 : 0x01));
            bout.write(0);
        }
        if (hasStarbase) {
            if (typeId == BlockType.PARTIAL_PLANET) {
                bout.write(starbaseDesign);
            } else {
                bout.write(starbaseBytes);
            }
        }
        if (hasRoute && typeId == BlockType.PLANET) {
            bout.write(routeShort & 0xFF);
            bout.write(routeShort >> 8);
        }
        if (turn >= 0) {
            bout.write(turn & 0xFF);
            bout.write(turn >> 8);
        }
        byte[] bytes = bout.toByteArray();
        setDecryptedData(bytes, bytes.length);
        setData(bytes, bytes.length);
        encrypted = false;
	}
	    
    private int getContentLength() {
        return 1 + byteLengthForInt(ironium) + byteLengthForInt(boranium) + byteLengthForInt(germanium)
                + byteLengthForInt(population);
    }
    
    private static int byteLengthForInt(long n) {
        if (n == 0) return 0;
        if (n < 256) return 1;
        if (n < 65536) return 2;
        return 4;
    }
    
    public static boolean isCompatible(PartialPlanetBlock first, PartialPlanetBlock second) {
        if (first == null || second == null) return true;
        if (first.owner != second.owner) return false;
        if (first.isHomeworld != second.isHomeworld) return false;
        if (first.hasStarbase && second.hasStarbase && (first.starbaseDesign != second.starbaseDesign)) return false;
        if (first.hasEnvironmentInfo && second.hasEnvironmentInfo) {
            if (first.ironiumConc != second.ironiumConc) return false;
            if (first.boraniumConc != second.boraniumConc) return false;
            if (first.germaniumConc != second.germaniumConc) return false;
            if (first.gravity != second.gravity) return false;
            if (first.temperature != second.temperature) return false;
            if (first.radiation != second.radiation) return false;
            if (first.isTerraformed != second.isTerraformed) return false;
            if (first.isTerraformed) {
                if (first.origGravity != second.origGravity) return false;
                if (first.origTemperature != second.origTemperature) return false;
                if (first.origRadiation != second.origRadiation) return false;
            }
            if (first.owner >= 0) {
                if (first.popEstimate != second.popEstimate) return false;
                if (first.defensesEstimate != second.defensesEstimate) return false;
            }
        }
        return true;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Planet " + planetNumber + ", Owner " + owner);
        if (isHomeworld) sb.append(", isHomeworld");
        if (isInUseOrRobberBaron) sb.append(", isInUseOrRobberBaron");
        if (hasEnvironmentInfo) sb.append(", hasEnvironmentInfo");
        if (bitWhichIsOffForRemoteMiningAndRobberBaron) sb.append(", bitWhichIsOffForRemoteMiningAndRobberBaron");
        if (weirdBit) sb.append(", weirdBit");
        if (hasRoute) sb.append(", hasRoute");
        if (hasSurfaceMinerals) sb.append(", hasSurfaceMinerals");
        if (hasArtifact) sb.append(", hasArtifact");
        if (hasInstallations) sb.append(", hasInstallations");
        if (isTerraformed) sb.append(", isTerraformed");
        if (hasStarbase) sb.append(", hasStarbase");
        sb.append("\n");
        sb.append(java.util.Arrays.toString(fractionalMinConcBytes));
        sb.append(", MinConc: " + ironiumConc + "/" + boraniumConc + "/" + germaniumConc);
        sb.append(", Hab: " + gravity + "/" + temperature + "/" + radiation);
        sb.append(", OrigHab: " + origGravity + "/" + origTemperature + "/" + origRadiation);
        sb.append("\n");
        sb.append("Estimates: " + (defensesEstimate * 100/16) + ";" + (popEstimate*400));
        sb.append(", Min: " + ironium + "/" + boranium + "/" + germanium);
        sb.append(", Pop: " + population + "(00+" + excessPop + ")");
        sb.append(", Inst: " + mines + "/" + factories + "/" + defenses + "/" + unknownInstallationsByte + "/" + contributeOnlyLeftoverResourcesToResearch + "/" + hasScanner);
        sb.append(", SB: " + starbaseDesign + "(" + java.util.Arrays.toString(starbaseBytes) + ")");
        sb.append(", RouteShort: " + routeShort);
        sb.append(", Turn: " + turn);
        return sb.toString();
    }

}
