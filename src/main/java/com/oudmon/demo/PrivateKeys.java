package com.oudmon.demo;

import java.math.BigInteger;
import java.net.InetAddress;
import java.security.interfaces.ECKey;

public class PrivateKeys {
    public static void main(String[] args) throws Exception {
        // TODO: Assumes main network not testnet. Make it selectable.
        NetworkParameters params = MainNetParams.get();
        try {
            // Decode the private key from Satoshis Base58 variant. If 51 characters long then it's from Bitcoins
            // dumpprivkey command and includes a version byte and checksum, or if 52 characters long then it has 
            // compressed pub key. Otherwise assume it's a raw key.
            ECKey key;
            if (args[0].length() == 51 || args[0].length() == 52) {
                DumpedPrivateKey dumpedPrivateKey = DumpedPrivateKey.fromBase58(params, args[0]);
                key = dumpedPrivateKey.getKey();
            } else {
                BigInteger privKey = Base58.decodeToBigInteger(args[0]);
                key = ECKey.fromPrivate(privKey);
            }
            System.out.println("Address from private key is: " + LegacyAddress.fromKey(params, key).toString());
            // And the address ...
            Address destination = LegacyAddress.fromBase58(params, args[1]);

            // Import the private key to a fresh wallet.
            Wallet wallet = new Wallet(params);
            wallet.importKey(key);

            // Find the transactions that involve those coins.
            final MemoryBlockStore blockStore = new MemoryBlockStore(params);
            BlockChain chain = new BlockChain(params, wallet, blockStore);

            final PeerGroup peerGroup = new PeerGroup(params, chain);
            peerGroup.addAddress(new PeerAddress(params, InetAddress.getLocalHost()));
            peerGroup.startAsync();
            peerGroup.downloadBlockChain();
            peerGroup.stopAsync();

            // And take them!
            System.out.println("Claiming " + wallet.getBalance().toFriendlyString());
            wallet.sendCoins(peerGroup, destination, wallet.getBalance());
            // Wait a few seconds to let the packets flush out to the network (ugly).
            Thread.sleep(5000);
            System.exit(0);
        } catch (ArrayIndexOutOfBoundsException e) {
            System.out.println("First arg should be private key in Base58 format. Second argument should be address " +
                    "to send to.");
        }
    }
}
