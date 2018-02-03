/*
 * Copyright 2013, 2014 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */

package com.mycelium.wallet;

import android.content.Context;

import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.net.ServerEndpoints;
import com.mycelium.wallet.activity.util.BlockExplorer;
import com.mycelium.wallet.external.BuySellServiceDescriptor;

import java.util.List;

public abstract class MbwEnvironment {

   private String _brand;

   public static MbwEnvironment verifyEnvironment(Context applicationContext) {
      // Set up environment
      String network = applicationContext.getResources().getString(R.string.network);
      String brand = applicationContext.getResources().getString(R.string.brand);
      if(brand.equals("undefined")){
         throw new RuntimeException("No brand has been specified");
      }
      // todo proper IoC needed. it is not nice to refer to subclasses
      if (network.equals("prodnet")) {
         return new MbwProdEnvironment(brand);
      } else if (network.equals("testnet")) {
         return new MbwTestEnvironment(brand);
      } else if (network.equals("regtest")) {
         return new MbwRegTestEnvironment(brand);
      } else {
         throw new RuntimeException("No network has been specified");
      }
   }

   public MbwEnvironment(String brand) {
      _brand = brand;
   }

   public String getBrand() {
      return _brand;
   }

   public abstract NetworkParameters getNetwork();
   public abstract ServerEndpoints getLtEndpoints();
   public abstract ServerEndpoints getWapiEndpoints();
   public abstract List<BlockExplorer> getBlockExplorerList();
   public abstract List<BuySellServiceDescriptor> getBuySellServices();

}
