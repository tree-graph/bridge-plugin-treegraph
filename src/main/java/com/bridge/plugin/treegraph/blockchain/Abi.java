package com.bridge.plugin.treegraph.blockchain;

import com.bridge.plugin.treegraph.service.CrossInfo;
import com.bridge.plugin.treegraph.service.CrossItem;
import conflux.web3j.response.Log;
import org.apache.tomcat.util.buf.HexUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.abi.FunctionReturnDecoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.*;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Abi {
    /*
    event CrossRequest(
            address indexed asset,
            address indexed from,
            uint[] tokenIds,
            uint[] amounts,
            string[] uris,
            uint toChainId,
            address targetContract,
            uint userNonce
        );
     */
    public static final Event CrossRequest = new Event("CrossRequest",
            Arrays.<TypeReference<?>>asList(
                    new TypeReference<Address>(true) {},//asset
                    new TypeReference<Address>(true) {},//from
                    new TypeReference<DynamicArray<Uint256>>(false) {},//tokenIds
                    new TypeReference<DynamicArray<Uint256>>(false) {},//amounts
                    new TypeReference<DynamicArray<Utf8String>>(false) {},//uris
                    new TypeReference<Uint256>(false) {},//toChainId
                    new TypeReference<Address>(false) {},//targetContract
                    new TypeReference<Uint256>(false) {}//userNonce
            )
    );
    public static void decodeCrossRequest(Log log, CrossInfo info) {
        List<String> topics = log.getTopics();
        info.asset = new Address(topics.get(1)).toString();
        info.from = new Address(topics.get(2)).toString();
        info.txnHash = log.getTransactionHash().get();
    }
    public static List<CrossItem> decodeCrossRequest(String encoded, CrossInfo info) {
        List<Type> result = FunctionReturnDecoder.decode(encoded,
                CrossRequest.getNonIndexedParameters());
        List<Uint256> tokenIds = (List<Uint256>) result.get(0).getValue();
        List<Uint256> amounts = (List<Uint256>) result.get(1).getValue();
        List<Utf8String> uris = (List<Utf8String>) result.get(2).getValue();
        BigInteger toChainId = (BigInteger) result.get(3).getValue();
        String targetContract = (String) result.get(4).getValue();
        BigInteger userNonce = (BigInteger) result.get(5).getValue();
        List<CrossItem> items = new ArrayList<>(tokenIds.size());
        for (int i = 0; i < tokenIds.size(); i++) {
            CrossItem ci = new CrossItem();
            ci.tokenId = Numeric.toHexString(tokenIds.get(i).getValue().toByteArray());
            ci.amount = Numeric.toHexString(amounts.get(i).getValue().toByteArray());
            ci.uri = uris.get(i).getValue();
            items.add(ci);
        }
        info.targetContract = targetContract;
        info.userNonce = userNonce.intValue();
        info.toChainId = toChainId.intValue();
//        debugCrossItems(tokenIds, amounts, uris, toChainId, targetContract, userNonce);
        return items;
    }

    private static void debugCrossItems(List<Uint256> tokenIds, List<Uint256> amounts, List<Utf8String> uris, BigInteger toChainId, String targetContract, BigInteger userNonce) {
        Logger log = LoggerFactory.getLogger("");
        log.info("tokenIds {}", tokenIds.stream().map(u->u.getValue()).collect(Collectors.toList()));
        log.info("amounts {}", amounts.stream().map(u->u.getValue()).collect(Collectors.toList()));
        log.info("uris {}", uris.stream().map(u->{
            byte[] value = u.getValue().getBytes();
            log.info("bytes {}", HexUtils.toHexString(value));
            String str = new String(value);
            return str +" Length "+str.length();
        }).collect(Collectors.toList()));
        log.info("toChainId {}", toChainId);
        log.info("targetContract {}", targetContract);
        log.info("userNonce {}", userNonce);
    }
}
