package sdk;

import conflux.web3j.types.AddressException;
import conflux.web3j.types.ConfluxBase32;
import org.apache.tomcat.util.buf.HexUtils;

import static conflux.web3j.grpc.Convert.*;
import static conflux.web3j.types.ConfluxBase32.createCheckSum;

public class Address {

    private static final byte VERSION_BYTE = 0x00;
    private static final int CHECKSUM_LEN = 8;
    private static final int HEX_BUFFER_LEN = 20;
    private static final int HEX_PREFIX_LEN = 2;
    private static final String HEX_PREFIX = "0X";
    private static final String DELIMITER = ":";
    private static final byte[] CHECKSUM_TEMPLATE = new byte[8];
    //    private static final long NET_ID_LIMIT = 4294967295L;  // 0xFFFFFFFF
    private static final int CFX_ADDRESS_CHAR_LENGTH = 42;

    public static String decode(String cfxAddress) throws AddressException {
        if(cfxAddress == null || !haveNetworkPrefix(cfxAddress)) {
            throw new AddressException("Invalid argument");
        }
        cfxAddress = cfxAddress.toLowerCase();
        String[] parts = cfxAddress.split(DELIMITER);
        if (parts.length < 2) {
            throw new AddressException("Address should have at least two part");
        }
        String network = parts[0];
        String payloadWithSum = parts[parts.length-1];
        if (!ConfluxBase32.isValid(payloadWithSum)) {
            throw new AddressException("Input contain invalid base32 chars");
        }
        if (payloadWithSum.length() != CFX_ADDRESS_CHAR_LENGTH) {
            throw new AddressException("Address payload should have 42 chars");
        }
        String sum = payloadWithSum.substring(payloadWithSum.length()-CHECKSUM_LEN);
        String payload = payloadWithSum.substring(0, payloadWithSum.length()-CHECKSUM_LEN);
        if (!sum.equals(createCheckSum(network, payload))) {
            throw new AddressException("Invalid checksum");
        }
        byte[] raw = ConfluxBase32.decode(payload);
        String hexAddress = HEX_PREFIX + HexUtils.toHexString(raw).substring(HEX_PREFIX_LEN);
        return hexAddress.toLowerCase();
    }
}