/**
 * Copyright (C) 2013-2014 Regents of the University of California.
 * @author: Jeff Thompson <jefft0@remap.ucla.edu>
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * A copy of the GNU General Public License is in the file COPYING.
 */

package net.named_data.jndn.tests;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.named_data.jndn.ContentType;
import net.named_data.jndn.Data;
import net.named_data.jndn.KeyLocatorType;
import net.named_data.jndn.Name;
import net.named_data.jndn.Sha256WithRsaSignature;
import net.named_data.jndn.encoding.TlvWireFormat;
import net.named_data.jndn.encoding.WireFormat;
import net.named_data.jndn.security.KeyChain;
import net.named_data.jndn.security.KeyType;
import net.named_data.jndn.security.OnVerified;
import net.named_data.jndn.security.OnVerifyFailed;
import net.named_data.jndn.security.identity.IdentityManager;
import net.named_data.jndn.security.identity.MemoryIdentityStorage;
import net.named_data.jndn.security.identity.MemoryPrivateKeyStorage;
import net.named_data.jndn.security.policy.SelfVerifyPolicyManager;
import net.named_data.jndn.util.Blob;

public class TestEncodeDecodeData {
  // Convert the int array to a ByteBuffer.
  private static ByteBuffer 
  toBuffer(int[] array) 
  {
    ByteBuffer result = ByteBuffer.allocate(array.length);
    for (int i = 0; i < array.length; ++i)
      result.put((byte)(array[i] & 0xff));
    
    result.flip();
    return result;
  }

  private static final ByteBuffer DEFAULT_PUBLIC_KEY_DER = toBuffer(new int[] {  
    0x30, 0x81, 0x9F, 0x30, 0x0D, 0x06, 0x09, 0x2A, 0x86, 0x48, 0x86, 0xF7, 0x0D, 0x01, 0x01, 0x01, 0x05, 0x00, 0x03, 0x81,
    0x8D, 0x00, 0x30, 0x81, 0x89, 0x02, 0x81, 0x81, 0x00, 0xE1, 0x7D, 0x30, 0xA7, 0xD8, 0x28, 0xAB, 0x1B, 0x84, 0x0B, 0x17,
    0x54, 0x2D, 0xCA, 0xF6, 0x20, 0x7A, 0xFD, 0x22, 0x1E, 0x08, 0x6B, 0x2A, 0x60, 0xD1, 0x6C, 0xB7, 0xF5, 0x44, 0x48, 0xBA,
    0x9F, 0x3F, 0x08, 0xBC, 0xD0, 0x99, 0xDB, 0x21, 0xDD, 0x16, 0x2A, 0x77, 0x9E, 0x61, 0xAA, 0x89, 0xEE, 0xE5, 0x54, 0xD3,
    0xA4, 0x7D, 0xE2, 0x30, 0xBC, 0x7A, 0xC5, 0x90, 0xD5, 0x24, 0x06, 0x7C, 0x38, 0x98, 0xBB, 0xA6, 0xF5, 0xDC, 0x43, 0x60,
    0xB8, 0x45, 0xED, 0xA4, 0x8C, 0xBD, 0x9C, 0xF1, 0x26, 0xA7, 0x23, 0x44, 0x5F, 0x0E, 0x19, 0x52, 0xD7, 0x32, 0x5A, 0x75,
    0xFA, 0xF5, 0x56, 0x14, 0x4F, 0x9A, 0x98, 0xAF, 0x71, 0x86, 0xB0, 0x27, 0x86, 0x85, 0xB8, 0xE2, 0xC0, 0x8B, 0xEA, 0x87,
    0x17, 0x1B, 0x4D, 0xEE, 0x58, 0x5C, 0x18, 0x28, 0x29, 0x5B, 0x53, 0x95, 0xEB, 0x4A, 0x17, 0x77, 0x9F, 0x02, 0x03, 0x01,
    0x00, 0x01  
  });

  private static final ByteBuffer DEFAULT_PRIVATE_KEY_DER = toBuffer(new int[] {  
    0x30, 0x82, 0x02, 0x77, 0x02, 0x01, 0x00, 0x30, 0x0d, 0x06, 0x09, 0x2a, 0x86, 0x48, 0x86, 0xf7, 0x0d, 0x01, 0x01, 0x01,
    0x05, 0x00, 0x04, 0x82, 0x02, 0x61, 0x30, 0x82, 0x02, 0x5d, 0x02, 0x01, 0x00, 0x02, 0x81, 0x81, 0x00, 0xe1, 0x7d, 0x30,
    0xa7, 0xd8, 0x28, 0xab, 0x1b, 0x84, 0x0b, 0x17, 0x54, 0x2d, 0xca, 0xf6, 0x20, 0x7a, 0xfd, 0x22, 0x1e, 0x08, 0x6b, 0x2a,
    0x60, 0xd1, 0x6c, 0xb7, 0xf5, 0x44, 0x48, 0xba, 0x9f, 0x3f, 0x08, 0xbc, 0xd0, 0x99, 0xdb, 0x21, 0xdd, 0x16, 0x2a, 0x77,
    0x9e, 0x61, 0xaa, 0x89, 0xee, 0xe5, 0x54, 0xd3, 0xa4, 0x7d, 0xe2, 0x30, 0xbc, 0x7a, 0xc5, 0x90, 0xd5, 0x24, 0x06, 0x7c,
    0x38, 0x98, 0xbb, 0xa6, 0xf5, 0xdc, 0x43, 0x60, 0xb8, 0x45, 0xed, 0xa4, 0x8c, 0xbd, 0x9c, 0xf1, 0x26, 0xa7, 0x23, 0x44,
    0x5f, 0x0e, 0x19, 0x52, 0xd7, 0x32, 0x5a, 0x75, 0xfa, 0xf5, 0x56, 0x14, 0x4f, 0x9a, 0x98, 0xaf, 0x71, 0x86, 0xb0, 0x27,
    0x86, 0x85, 0xb8, 0xe2, 0xc0, 0x8b, 0xea, 0x87, 0x17, 0x1b, 0x4d, 0xee, 0x58, 0x5c, 0x18, 0x28, 0x29, 0x5b, 0x53, 0x95,
    0xeb, 0x4a, 0x17, 0x77, 0x9f, 0x02, 0x03, 0x01, 0x00, 0x01, 0x02, 0x81, 0x80, 0x1a, 0x4b, 0xfa, 0x4f, 0xa8, 0xc2, 0xdd,
    0x69, 0xa1, 0x15, 0x96, 0x0b, 0xe8, 0x27, 0x42, 0x5a, 0xf9, 0x5c, 0xea, 0x0c, 0xac, 0x98, 0xaa, 0xe1, 0x8d, 0xaa, 0xeb,
    0x2d, 0x3c, 0x60, 0x6a, 0xfb, 0x45, 0x63, 0xa4, 0x79, 0x83, 0x67, 0xed, 0xe4, 0x15, 0xc0, 0xb0, 0x20, 0x95, 0x6d, 0x49,
    0x16, 0xc6, 0x42, 0x05, 0x48, 0xaa, 0xb1, 0xa5, 0x53, 0x65, 0xd2, 0x02, 0x99, 0x08, 0xd1, 0x84, 0xcc, 0xf0, 0xcd, 0xea,
    0x61, 0xc9, 0x39, 0x02, 0x3f, 0x87, 0x4a, 0xe5, 0xc4, 0xd2, 0x07, 0x02, 0xe1, 0x9f, 0xa0, 0x06, 0xc2, 0xcc, 0x02, 0xe7,
    0xaa, 0x6c, 0x99, 0x8a, 0xf8, 0x49, 0x00, 0xf1, 0xa2, 0x8c, 0x0c, 0x8a, 0xb9, 0x4f, 0x6d, 0x73, 0x3b, 0x2c, 0xb7, 0x9f,
    0x8a, 0xa6, 0x7f, 0x9b, 0x9f, 0xb7, 0xa1, 0xcc, 0x74, 0x2e, 0x8f, 0xb8, 0xb0, 0x26, 0x89, 0xd2, 0xe5, 0x66, 0xe8, 0x8e,
    0xa1, 0x02, 0x41, 0x00, 0xfc, 0xe7, 0x52, 0xbc, 0x4e, 0x95, 0xb6, 0x1a, 0xb4, 0x62, 0xcc, 0xd8, 0x06, 0xe1, 0xdc, 0x7a,
    0xa2, 0xb6, 0x71, 0x01, 0xaa, 0x27, 0xfc, 0x99, 0xe5, 0xf2, 0x54, 0xbb, 0xb2, 0x85, 0xe1, 0x96, 0x54, 0x2d, 0xcb, 0xba,
    0x86, 0xfa, 0x80, 0xdf, 0xcf, 0x39, 0xe6, 0x74, 0xcb, 0x22, 0xce, 0x70, 0xaa, 0x10, 0x00, 0x73, 0x1d, 0x45, 0x0a, 0x39,
    0x51, 0x84, 0xf5, 0x15, 0x8f, 0x37, 0x76, 0x91, 0x02, 0x41, 0x00, 0xe4, 0x3f, 0xf0, 0xf4, 0xde, 0x79, 0x77, 0x48, 0x9b,
    0x9c, 0x28, 0x45, 0x26, 0x57, 0x3c, 0x71, 0x40, 0x28, 0x6a, 0xa1, 0xfe, 0xc3, 0xe5, 0x37, 0xa1, 0x03, 0xf6, 0x2d, 0xbe,
    0x80, 0x64, 0x72, 0x69, 0x2e, 0x9b, 0x4d, 0xe3, 0x2e, 0x1b, 0xfe, 0xe7, 0xf9, 0x77, 0x8c, 0x18, 0x53, 0x9f, 0xe2, 0xfe,
    0x00, 0xbb, 0x49, 0x20, 0x47, 0xdf, 0x01, 0x61, 0x87, 0xd6, 0xe3, 0x44, 0xb5, 0x03, 0x2f, 0x02, 0x40, 0x54, 0xec, 0x7c,
    0xbc, 0xdd, 0x0a, 0xaa, 0xde, 0xe6, 0xc9, 0xf2, 0x8d, 0x6c, 0x2a, 0x35, 0xf6, 0x3c, 0x63, 0x55, 0x29, 0x40, 0xf1, 0x32,
    0x82, 0x9f, 0x53, 0xb3, 0x9e, 0x5f, 0xc1, 0x53, 0x52, 0x3e, 0xac, 0x2e, 0x28, 0x51, 0xa1, 0x16, 0xdb, 0x90, 0xe3, 0x99,
    0x7e, 0x88, 0xa4, 0x04, 0x7c, 0x92, 0xae, 0xd2, 0xe7, 0xd4, 0xe1, 0x55, 0x20, 0x90, 0x3e, 0x3c, 0x6a, 0x63, 0xf0, 0x34,
    0xf1, 0x02, 0x41, 0x00, 0x84, 0x5a, 0x17, 0x6c, 0xc6, 0x3c, 0x84, 0xd0, 0x93, 0x7a, 0xff, 0x56, 0xe9, 0x9e, 0x98, 0x2b,
    0xcb, 0x5a, 0x24, 0x4a, 0xff, 0x21, 0xb4, 0x9e, 0x87, 0x3d, 0x76, 0xd8, 0x9b, 0xa8, 0x73, 0x96, 0x6c, 0x2b, 0x5c, 0x5e,
    0xd3, 0xa6, 0xff, 0x10, 0xd6, 0x8e, 0xaf, 0xa5, 0x8a, 0xcd, 0xa2, 0xde, 0xcb, 0x0e, 0xbd, 0x8a, 0xef, 0xae, 0xfd, 0x3f,
    0x1d, 0xc0, 0xd8, 0xf8, 0x3b, 0xf5, 0x02, 0x7d, 0x02, 0x41, 0x00, 0x8b, 0x26, 0xd3, 0x2c, 0x7d, 0x28, 0x38, 0x92, 0xf1,
    0xbf, 0x15, 0x16, 0x39, 0x50, 0xc8, 0x6d, 0x32, 0xec, 0x28, 0xf2, 0x8b, 0xd8, 0x70, 0xc5, 0xed, 0xe1, 0x7b, 0xff, 0x2d,
    0x66, 0x8c, 0x86, 0x77, 0x43, 0xeb, 0xb6, 0xf6, 0x50, 0x66, 0xb0, 0x40, 0x24, 0x6a, 0xaf, 0x98, 0x21, 0x45, 0x30, 0x01,
    0x59, 0xd0, 0xc3, 0xfc, 0x7b, 0xae, 0x30, 0x18, 0xeb, 0x90, 0xfb, 0x17, 0xd3, 0xce, 0xb5
  });
  
  private static final ByteBuffer BinaryXmlData = toBuffer(new int[] {
0x04, 0x82, // NDN Data
  0x02, 0xaa, // Signature
    0x03, 0xb2, // SignatureBits
      0x08, 0x85, 0x20, 0xea, 0xb5, 0xb0, 0x63, 0xda, 0x94, 0xe9, 0x68, 0x7a,
      0x8e, 0x65, 0x60, 0xe0, 0xc6, 0x43, 0x96, 0xd9, 0x69, 0xb4, 0x40, 0x72, 0x52, 0x00, 0x2c, 0x8e, 0x2a, 0xf5,
      0x47, 0x12, 0x59, 0x93, 0xda, 0xed, 0x82, 0xd0, 0xf8, 0xe6, 0x65, 0x09, 0x87, 0x84, 0x54, 0xc7, 0xce, 0x9a,
      0x93, 0x0d, 0x47, 0xf1, 0xf9, 0x3b, 0x98, 0x78, 0x2c, 0x22, 0x21, 0xd9, 0x2b, 0xda, 0x03, 0x30, 0x84, 0xf3,
      0xc5, 0x52, 0x64, 0x2b, 0x1d, 0xde, 0x50, 0xe0, 0xee, 0xca, 0xa2, 0x73, 0x7a, 0x93, 0x30, 0xa8, 0x47, 0x7f,
      0x6f, 0x41, 0xb0, 0xc8, 0x6e, 0x89, 0x1c, 0xcc, 0xf9, 0x01, 0x44, 0xc3, 0x08, 0xcf, 0x77, 0x47, 0xfc, 0xed,
      0x48, 0xf0, 0x4c, 0xe9, 0xc2, 0x3b, 0x7d, 0xef, 0x6e, 0xa4, 0x80, 0x40, 0x9e, 0x43, 0xb6, 0x77, 0x7a, 0x1d,
      0x51, 0xed, 0x98, 0x33, 0x93, 0xdd, 0x88, 0x01, 0x0e, 0xd3, 
    0x00, 
  0x00, 
  0xf2, 0xfa, 0x9d, 0x6e, 0x64, 0x6e, 0x00, 0xfa, 0x9d, 0x61, 0x62, 0x63, 0x00, 0x00,  // Name
  0x01, 0xa2, // SignedInfo
    0x03, 0xe2, // PublisherPublicKeyDigest
      0x02, 0x85, 0xb5, 0x50, 0x6b, 0x1a,
      0xba, 0x3d, 0xa7, 0x76, 0x1b, 0x0f, 0x8d, 0x61, 0xa4, 0xaa, 0x7e, 0x3b, 0x6d, 0x15, 0xb4, 0x26, 0xfe, 0xb5,
      0xbd, 0xa8, 0x23, 0x89, 0xac, 0xa7, 0x65, 0xa3, 0xb8, 0x1c, 
    0x00, 
    0x02, 0xba, // Timestamp
      0xb5, 0x05, 0x1d, 0xde, 0xe9, 0x5b, 0xdb, 
    0x00, 
    0x01, 0xe2, // KeyLocator
      0x01, 0xda, // Key
        0x0a, 0x95, 0x30, 0x81, 0x9f, 0x30, 0x0d, 0x06, 0x09, 0x2a, 0x86,
        0x48, 0x86, 0xf7, 0x0d, 0x01, 0x01, 0x01, 0x05, 0x00, 0x03, 0x81, 0x8d, 0x00, 0x30, 0x81, 0x89, 0x02, 0x81,
        0x81, 0x00, 0xe1, 0x7d, 0x30, 0xa7, 0xd8, 0x28, 0xab, 0x1b, 0x84, 0x0b, 0x17, 0x54, 0x2d, 0xca, 0xf6, 0x20,
        0x7a, 0xfd, 0x22, 0x1e, 0x08, 0x6b, 0x2a, 0x60, 0xd1, 0x6c, 0xb7, 0xf5, 0x44, 0x48, 0xba, 0x9f, 0x3f, 0x08,
        0xbc, 0xd0, 0x99, 0xdb, 0x21, 0xdd, 0x16, 0x2a, 0x77, 0x9e, 0x61, 0xaa, 0x89, 0xee, 0xe5, 0x54, 0xd3, 0xa4,
        0x7d, 0xe2, 0x30, 0xbc, 0x7a, 0xc5, 0x90, 0xd5, 0x24, 0x06, 0x7c, 0x38, 0x98, 0xbb, 0xa6, 0xf5, 0xdc, 0x43,
        0x60, 0xb8, 0x45, 0xed, 0xa4, 0x8c, 0xbd, 0x9c, 0xf1, 0x26, 0xa7, 0x23, 0x44, 0x5f, 0x0e, 0x19, 0x52, 0xd7,
        0x32, 0x5a, 0x75, 0xfa, 0xf5, 0x56, 0x14, 0x4f, 0x9a, 0x98, 0xaf, 0x71, 0x86, 0xb0, 0x27, 0x86, 0x85, 0xb8,
        0xe2, 0xc0, 0x8b, 0xea, 0x87, 0x17, 0x1b, 0x4d, 0xee, 0x58, 0x5c, 0x18, 0x28, 0x29, 0x5b, 0x53, 0x95, 0xeb,
        0x4a, 0x17, 0x77, 0x9f, 0x02, 0x03, 0x01, 0x00, 0x01, 
      0x00, 
    0x00, 
  0x00, 
  0x01, 0x9a, // Content
    0xc5, 0x53, 0x55, 0x43, 0x43, 0x45, 0x53, 0x53, 0x21, 
  0x00, 
0x00,
1
  });
  
  private static final ByteBuffer TlvData = toBuffer(new int[] {
0x06, 0xCE, // NDN Data
  0x07, 0x0A, 0x08, 0x03, 0x6E, 0x64, 0x6E, 0x08, 0x03, 0x61, 0x62, 0x63, // Name
  0x14, 0x0A, // MetaInfo
    0x19, 0x02, 0x13, 0x88, // FreshnessPeriod
    0x1A, 0x04, // FinalBlockId
      0x08, 0x02, 0x00, 0x09, // NameComponent
  0x15, 0x08, 0x53, 0x55, 0x43, 0x43, 0x45, 0x53, 0x53, 0x21, // Content
  0x16, 0x28, // SignatureInfo
    0x1B, 0x01, 0x01, // SignatureType
    0x1C, 0x23, // KeyLocator
      0x07, 0x21, // Name
        0x08, 0x08, 0x74, 0x65, 0x73, 0x74, 0x6E, 0x61, 0x6D, 0x65,
        0x08, 0x03, 0x4B, 0x45, 0x59,
        0x08, 0x07, 0x44, 0x53, 0x4B, 0x2D, 0x31, 0x32, 0x33,
        0x08, 0x07, 0x49, 0x44, 0x2D, 0x43, 0x45, 0x52, 0x54,
  0x17, 0x80, // SignatureValue
    0x1A, 0x03, 0xC3, 0x9C, 0x4F, 0xC5, 0x5C, 0x36, 0xA2, 0xE7, 0x9C, 0xEE, 0x52, 0xFE, 0x45, 0xA7, 
    0xE1, 0x0C, 0xFB, 0x95, 0xAC, 0xB4, 0x9B, 0xCC, 0xB6, 0xA0, 0xC3, 0x4A, 0xAA, 0x45, 0xBF, 0xBF, 
    0xDF, 0x0B, 0x51, 0xD5, 0xA4, 0x8B, 0xF2, 0xAB, 0x45, 0x97, 0x1C, 0x24, 0xD8, 0xE2, 0xC2, 0x8A, 
    0x4D, 0x40, 0x12, 0xD7, 0x77, 0x01, 0xEB, 0x74, 0x35, 0xF1, 0x4D, 0xDD, 0xD0, 0xF3, 0xA6, 0x9A, 
    0xB7, 0xA4, 0xF1, 0x7F, 0xA7, 0x84, 0x34, 0xD7, 0x08, 0x25, 0x52, 0x80, 0x8B, 0x6C, 0x42, 0x93, 
    0x04, 0x1E, 0x07, 0x1F, 0x4F, 0x76, 0x43, 0x18, 0xF2, 0xF8, 0x51, 0x1A, 0x56, 0xAF, 0xE6, 0xA9, 
    0x31, 0xCB, 0x6C, 0x1C, 0x0A, 0xA4, 0x01, 0x10, 0xFC, 0xC8, 0x66, 0xCE, 0x2E, 0x9C, 0x0B, 0x2D, 
    0x7F, 0xB4, 0x64, 0xA0, 0xEE, 0x22, 0x82, 0xC8, 0x34, 0xF7, 0x9A, 0xF5, 0x51, 0x12, 0x2A, 0x84,
1
  });

  private static void 
  dumpData(Data data)
  {
    System.out.println("name: " + data.getName().toUri());
    if (data.getContent().size() > 0) {
      System.out.print("content (raw): ");
      ByteBuffer buf = data.getContent().buf();
      while(buf.remaining() > 0)
        System.out.print((char)buf.get());
      System.out.println();
      System.out.println("content (hex): " + data.getContent().toHex());
    }
    else
      System.out.println("content: <empty>");

    if (!(data.getMetaInfo().getType() == ContentType.BLOB || 
          data.getMetaInfo().getType() == ContentType.DATA)) {
      System.out.print("metaInfo.type: ");
      if (data.getMetaInfo().getType() == ContentType.ENCR)
        System.out.println("ENCR");
      else if (data.getMetaInfo().getType() == ContentType.GONE)
        System.out.println("GONE");
      else if (data.getMetaInfo().getType() == ContentType.KEY)
        System.out.println("KEY");
      else if (data.getMetaInfo().getType() == ContentType.LINK)
        System.out.println("LINK");
      else if (data.getMetaInfo().getType() == ContentType.NACK)
        System.out.println("NACK");
    }
    System.out.println("metaInfo.freshnessPeriod (milliseconds): " +
      (data.getMetaInfo().getFreshnessPeriod() >= 0 ?
        "" + data.getMetaInfo().getFreshnessPeriod() : "<none>"));
    System.out.println("metaInfo.finalBlockID: " +
      (data.getMetaInfo().getFinalBlockID().getValue().size() > 0 ? 
       data.getMetaInfo().getFinalBlockID().getValue().toHex() : "<none>"));

    if (data.getSignature() instanceof Sha256WithRsaSignature) {
      Sha256WithRsaSignature signature = 
        (Sha256WithRsaSignature)data.getSignature();
      System.out.println("signature.signature: " +
        (signature.getSignature().size() > 0 ? 
         signature.getSignature().toHex() : "<none>"));
      System.out.print("signature.keyLocator: ");
      if (signature.getKeyLocator().getType() == KeyLocatorType.NONE)
        System.out.println("<none>");
      else if (signature.getKeyLocator().getType() == KeyLocatorType.KEY)
        System.out.println("Key: " + signature.getKeyLocator().getKeyData().toHex());
      else if (signature.getKeyLocator().getType() == KeyLocatorType.CERTIFICATE)
        System.out.println("Certificate: " + signature.getKeyLocator().getKeyData().toHex());
      else if (signature.getKeyLocator().getType() ==KeyLocatorType.KEY_LOCATOR_DIGEST)
        System.out.println("KeyLocatorDigest: " + signature.getKeyLocator().getKeyData().toHex());
      else if (signature.getKeyLocator().getType() == KeyLocatorType.KEYNAME)
        System.out.println("KeyName: " + signature.getKeyLocator().getKeyName().toUri());
      else
        System.out.println("<unrecognized ndn_KeyLocatorType>");
    }
  }
  
  private static class VerifyCallbacks implements OnVerified, OnVerifyFailed {
    public VerifyCallbacks(String prefix) { prefix_ = prefix; }
    
    private String prefix_;

    public void onVerified(Data data) 
    {
      System.out.println(prefix_ + " signature verification: VERIFIED");
    }

    public void onVerifyFailed(Data data) 
    {
      System.out.println(prefix_ + " signature verification: FAILED");
    }
  }
  
  public static void 
  main(String[] args) 
  {
    try {
      // Don't show INFO log messages.
      Logger.getLogger("").setLevel(Level.WARNING);
      
      Data data = new Data();
      // Note: While we transition to the TLV wire format, check if it has been made the default.
      if (WireFormat.getDefaultWireFormat() == TlvWireFormat.get())
        data.wireDecode(new Blob(TlvData, false));
      else
        data.wireDecode(new Blob(BinaryXmlData, false));
      System.out.println("Decoded Data:");
      dumpData(data);

      // Set the content again to clear the cached encoding so we encode again.
      data.setContent(data.getContent());
      Blob encoding = data.wireEncode();

      Data reDecodedData = new Data();
      reDecodedData.wireDecode(encoding);
      System.out.println();
      System.out.println("Re-decoded Data:");
      dumpData(reDecodedData);

      Data freshData = new Data(new Name("/ndn/abc"));
      freshData.setContent(new Blob("SUCCESS!"));
      freshData.getMetaInfo().setFreshnessPeriod(5000);
      freshData.getMetaInfo().setFinalBlockID(new Name("/%00%09").get(0));

      MemoryIdentityStorage identityStorage = new MemoryIdentityStorage();
      MemoryPrivateKeyStorage privateKeyStorage = new MemoryPrivateKeyStorage();
      KeyChain keyChain = new KeyChain
        (new IdentityManager(identityStorage, privateKeyStorage), 
         new SelfVerifyPolicyManager(identityStorage));

      // Initialize the storage.
      Name keyName = new Name("/testname/DSK-123");
      Name certificateName = keyName.getSubName(0, keyName.size() - 1).append
        ("KEY").append(keyName.get(-1)).append("ID-CERT").append("0");
      identityStorage.addKey(keyName, KeyType.RSA, new Blob(DEFAULT_PUBLIC_KEY_DER, false));
      privateKeyStorage.setKeyPairForKeyName
        (keyName, DEFAULT_PUBLIC_KEY_DER, DEFAULT_PRIVATE_KEY_DER);

      keyChain.sign(freshData, certificateName);
      System.out.println();
      System.out.println("Freshly-signed Data:");
      dumpData(freshData);

      VerifyCallbacks callbacks = new VerifyCallbacks("Freshly-signed Data");
      keyChain.verifyData(freshData, callbacks, callbacks);
    } 
    catch (Exception e) {
      System.out.println(e.getMessage());
    }
  }
}
