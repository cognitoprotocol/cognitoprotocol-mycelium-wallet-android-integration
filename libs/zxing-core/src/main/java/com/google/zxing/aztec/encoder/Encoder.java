/*
 * Copyright 2013 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.zxing.aztec.encoder;

import com.google.zxing.common.BitArray;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.reedsolomon.GenericGF;
import com.google.zxing.common.reedsolomon.ReedSolomonEncoder;

/**
 * Generates Aztec 2D barcodes.
 *
 * @author Rustam Abdullaev
 */
public final class Encoder {

  public static final int DEFAULT_EC_PERCENT = 33; // default minimal percentage of error check words
  private static final int[] NB_BITS; // total bits per compact symbol for a given number of layers
  private static final int[] NB_BITS_COMPACT; // total bits per full symbol for a given number of layers

  static {
    NB_BITS_COMPACT = new int[5];
    for (int i = 1; i < NB_BITS_COMPACT.length; i++) {
      NB_BITS_COMPACT[i] = (88 + 16 * i) * i;
    }
    NB_BITS = new int[33];
    for (int i = 1; i < NB_BITS.length; i++) {
      NB_BITS[i] = (112 + 16 * i) * i;
    }
  }
  
  private static final int[] WORD_SIZE = {
    4, 6, 6, 8, 8, 8, 8, 8, 8, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10, 10,
    12, 12, 12, 12, 12, 12, 12, 12, 12, 12
  };

  private Encoder() {
  }

  /**
   * Encodes the given binary content as an Aztec symbol
   * 
   * @param data input data string
   * @return Aztec symbol matrix with metadata
   */
  public static AztecCode encode(byte[] data) {
    return encode(data, DEFAULT_EC_PERCENT);
  }
  
  /**
   * Encodes the given binary content as an Aztec symbol
   * 
   * @param data input data string
   * @param minECCPercent minimal percentange of error check words (According to ISO/IEC 24778:2008,
   * a minimum of 23% + 3 words is recommended)
   * @return Aztec symbol matrix with metadata
   */
  public static AztecCode encode(byte[] data, int minECCPercent) {
    
    // High-level encode
    BitArray bits = new HighLevelEncoder(data).encode();
    
    // stuff bits and choose symbol size
    int eccBits = bits.getSize() * minECCPercent / 100 + 11;
    int totalSizeBits = bits.getSize() + eccBits;
    int layers;
    int wordSize = 0;
    int totalSymbolBits = 0;
    BitArray stuffedBits = null;
    for (layers = 1; layers < NB_BITS_COMPACT.length; layers++) {
      if (NB_BITS_COMPACT[layers] >= totalSizeBits) {
        if (wordSize != WORD_SIZE[layers]) {
          wordSize = WORD_SIZE[layers];
          stuffedBits = stuffBits(bits, wordSize);
        }
        totalSymbolBits = NB_BITS_COMPACT[layers];
        if (stuffedBits.getSize() + eccBits <= NB_BITS_COMPACT[layers]) {
          break;
        }
      }
    }
    boolean compact = true;
    if (layers == NB_BITS_COMPACT.length) {
      compact = false;
      for (layers = 1; layers < NB_BITS.length; layers++) {
        if (NB_BITS[layers] >= totalSizeBits) {
          if (wordSize != WORD_SIZE[layers]) {
            wordSize = WORD_SIZE[layers];
            stuffedBits = stuffBits(bits, wordSize);
          }
          totalSymbolBits = NB_BITS[layers];
          if (stuffedBits.getSize() + eccBits <= NB_BITS[layers]) {
            break;
          }
        }
      }
    }
    if (layers == NB_BITS.length) {
      throw new IllegalArgumentException("Data too large for an Aztec code");
    }

    // pad the end
    int messageSizeInWords = (stuffedBits.getSize() + wordSize - 1) / wordSize;
    for (int i = messageSizeInWords * wordSize - stuffedBits.getSize(); i > 0; i--) {
      stuffedBits.appendBit(true);
    }

    // generate check words
    ReedSolomonEncoder rs = new ReedSolomonEncoder(getGF(wordSize));
    int totalSizeInFullWords = totalSymbolBits / wordSize;
    int[] messageWords = bitsToWords(stuffedBits, wordSize, totalSizeInFullWords);
    rs.encode(messageWords, totalSizeInFullWords - messageSizeInWords);
    
    // convert to bit array and pad in the beginning
    int startPad = totalSymbolBits % wordSize;
    BitArray messageBits = new BitArray();
    messageBits.appendBits(0, startPad);
    for (int messageWord : messageWords) {
      messageBits.appendBits(messageWord, wordSize);
    }
    
    // generate mode message
    BitArray modeMessage = generateModeMessage(compact, layers, messageSizeInWords);

    // allocate symbol
    int baseMatrixSize = compact ? 11 + layers * 4 : 14 + layers * 4; // not including alignment lines
    int[] alignmentMap = new int[baseMatrixSize];
    int matrixSize;
    if (compact) {
      // no alignment marks in compact mode, alignmentMap is a no-op
      matrixSize = baseMatrixSize;
      for (int i = 0; i < alignmentMap.length; i++) {
        alignmentMap[i] = i;
      }
    } else {
      matrixSize = baseMatrixSize + 1 + 2 * ((baseMatrixSize / 2 - 1) / 15);
      int origCenter = baseMatrixSize / 2;
      int center = matrixSize / 2;
      for (int i = 0; i < origCenter; i++) {
        int newOffset = i + i / 15;
        alignmentMap[origCenter - i - 1] = center - newOffset - 1;
        alignmentMap[origCenter + i] = center + newOffset + 1;
      }
    }
    BitMatrix matrix = new BitMatrix(matrixSize);
    
    // draw mode and data bits
    for (int i = 0, rowOffset = 0; i < layers; i++) {
      int rowSize = compact ? (layers - i) * 4 + 9 : (layers - i) * 4 + 12;
      for (int j = 0; j < rowSize; j++) {
        int columnOffset = j * 2;
        for (int k = 0; k < 2; k++) {
          if (messageBits.get(rowOffset + columnOffset + k)) {
            matrix.set(alignmentMap[i * 2 + k], alignmentMap[i * 2 + j]);
          }
          if (messageBits.get(rowOffset + rowSize * 2 + columnOffset + k)) {
            matrix.set(alignmentMap[i * 2 + j], alignmentMap[baseMatrixSize - 1 - i * 2 - k]);
          }
          if (messageBits.get(rowOffset + rowSize * 4 + columnOffset + k)) {
            matrix.set(alignmentMap[baseMatrixSize - 1 - i * 2 - k], alignmentMap[baseMatrixSize - 1 - i * 2 - j]);
          }
          if (messageBits.get(rowOffset + rowSize * 6 + columnOffset + k)) {
            matrix.set(alignmentMap[baseMatrixSize - 1 - i * 2 - j], alignmentMap[i * 2 + k]);
          }
        }
      }
      rowOffset += rowSize * 8;
    }
    drawModeMessage(matrix, compact, matrixSize, modeMessage);
    
    // draw alignment marks
    if (compact) {
      drawBullsEye(matrix, matrixSize / 2, 5);
    } else {
      drawBullsEye(matrix, matrixSize / 2, 7);
      for (int i = 0, j = 0; i < baseMatrixSize / 2 - 1; i += 15, j += 16) {
        for (int k = (matrixSize / 2) & 1; k < matrixSize; k += 2) {
          matrix.set(matrixSize / 2 - j, k);
          matrix.set(matrixSize / 2 + j, k);
          matrix.set(k, matrixSize / 2 - j);
          matrix.set(k, matrixSize / 2 + j);
        }
      }
    }
    
    AztecCode aztec = new AztecCode();
    aztec.setCompact(compact);
    aztec.setSize(matrixSize);
    aztec.setLayers(layers);
    aztec.setCodeWords(messageSizeInWords);
    aztec.setMatrix(matrix);
    return aztec;
  }
  
  private static void drawBullsEye(BitMatrix matrix, int center, int size) {
    for (int i = 0; i < size; i += 2) {
      for (int j = center - i; j <= center + i; j++) {
        matrix.set(j, center - i);
        matrix.set(j, center + i);
        matrix.set(center - i, j);
        matrix.set(center + i, j);
      }
    }
    matrix.set(center - size, center - size);
    matrix.set(center - size + 1, center - size);
    matrix.set(center - size, center - size + 1);
    matrix.set(center + size, center - size);
    matrix.set(center + size, center - size + 1);
    matrix.set(center + size, center + size - 1);
  }
  
  static BitArray generateModeMessage(boolean compact, int layers, int messageSizeInWords) {
    BitArray modeMessage = new BitArray();
    if (compact) {
      modeMessage.appendBits(layers - 1, 2);
      modeMessage.appendBits(messageSizeInWords - 1, 6);
      modeMessage = generateCheckWords(modeMessage, 28, 4);
    } else {
      modeMessage.appendBits(layers - 1, 5);
      modeMessage.appendBits(messageSizeInWords - 1, 11);
      modeMessage = generateCheckWords(modeMessage, 40, 4);
    }
    return modeMessage;
  }
  
  private static void drawModeMessage(BitMatrix matrix, boolean compact, int matrixSize, BitArray modeMessage) {
    if (compact) {
      for (int i = 0; i < 7; i++) {
        if (modeMessage.get(i)) {
          matrix.set(matrixSize / 2 - 3 + i, matrixSize / 2 - 5);
        }
        if (modeMessage.get(i + 7)) {
          matrix.set(matrixSize / 2 + 5, matrixSize / 2 - 3 + i);
        }
        if (modeMessage.get(20 - i)) {
          matrix.set(matrixSize / 2 - 3 + i, matrixSize / 2 + 5);
        }
        if (modeMessage.get(27 - i)) {
          matrix.set(matrixSize / 2 - 5, matrixSize / 2 - 3 + i);
        }
      }
    } else {
      for (int i = 0; i < 10; i++) {
        if (modeMessage.get(i)) {
          matrix.set(matrixSize / 2 - 5 + i + i / 5, matrixSize / 2 - 7);
        }
        if (modeMessage.get(i + 10)) {
          matrix.set(matrixSize / 2 + 7, matrixSize / 2 - 5 + i + i / 5);
        }
        if (modeMessage.get(29 - i)) {
          matrix.set(matrixSize / 2 - 5 + i + i / 5, matrixSize / 2 + 7);
        }
        if (modeMessage.get(39 - i)) {
          matrix.set(matrixSize / 2 - 7, matrixSize / 2 - 5 + i + i / 5);
        }
      }
    }
  }
  
  private static BitArray generateCheckWords(BitArray stuffedBits, int totalSymbolBits, int wordSize) {
    int messageSizeInWords = (stuffedBits.getSize() + wordSize - 1) / wordSize;
    for (int i = messageSizeInWords * wordSize - stuffedBits.getSize(); i > 0; i--) {
      stuffedBits.appendBit(true);
    }
    ReedSolomonEncoder rs = new ReedSolomonEncoder(getGF(wordSize));
    int totalSizeInFullWords = totalSymbolBits / wordSize;
    int[] messageWords = bitsToWords(stuffedBits, wordSize, totalSizeInFullWords);
    rs.encode(messageWords, totalSizeInFullWords - messageSizeInWords);
    int startPad = totalSymbolBits % wordSize;
    BitArray messageBits = new BitArray();
    messageBits.appendBits(0, startPad);
    for (int messageWord : messageWords) {
      messageBits.appendBits(messageWord, wordSize);
    }
    return messageBits;
  }
  
  private static int[] bitsToWords(BitArray stuffedBits, int wordSize, int totalWords) {
    int[] message = new int[totalWords];
    int i;
    int n;
    for (i = 0, n = stuffedBits.getSize() / wordSize; i < n; i++) {
      int value = 0;
      for (int j = 0; j < wordSize; j++) {
        value |= stuffedBits.get(i * wordSize + j) ? (1 << wordSize - j - 1) : 0;
      }
      message[i] = value;
    }
    return message;
  }
  
  private static GenericGF getGF(int wordSize) {
    switch (wordSize) {
      case 4:
        return GenericGF.AZTEC_PARAM;
      case 6:
        return GenericGF.AZTEC_DATA_6;
      case 8:
        return GenericGF.AZTEC_DATA_8;
      case 10:
        return GenericGF.AZTEC_DATA_10;
      case 12:
        return GenericGF.AZTEC_DATA_12;
      default:
        return null;
    }
  }

  static BitArray stuffBits(BitArray bits, int wordSize) {
    BitArray out = new BitArray();

    // 1. stuff the bits
    int n = bits.getSize();
    int mask = (1 << wordSize) - 2;
    for (int i = 0; i < n; i += wordSize) {
      int word = 0;
      for (int j = 0; j < wordSize; j++) {
        if (i + j >= n || bits.get(i + j)) {
          word |= 1 << (wordSize - 1 - j);
        }
      }
      if ((word & mask) == mask) {
        out.appendBits(word & mask, wordSize);
        i--;
      } else if ((word & mask) == 0) {
        out.appendBits(word | 1, wordSize);
        i--;
      } else {
        out.appendBits(word, wordSize);
      }
    }
    
    // 2. pad last word to wordSize
    n = out.getSize();
    int remainder = n % wordSize;
    if (remainder != 0) {
      int j = 1;
      for (int i = 0; i < remainder; i++) {
        if (!out.get(n - 1 - i)) {
          j = 0;
        }
      }
      for (int i = remainder; i < wordSize - 1; i++) {
        out.appendBit(true);
      }
      out.appendBit(j == 0);
    }
    return out;
  }
}
