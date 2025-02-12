package com.pasc.lib.sm;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.math.ec.ECPoint;

import java.math.BigInteger;

/**
 * @Author: yuhaitao
 * @Date: 2019/2/28 17:03
 */
public class SM2Utils {

    //生成随机秘钥对
    public static void generateKeyPair(){
        SM2 sm2 = SM2.Instance();
        AsymmetricCipherKeyPair key = sm2.ecc_key_pair_generator.generateKeyPair();
        ECPrivateKeyParameters ecpriv = (ECPrivateKeyParameters) key.getPrivate();
        ECPublicKeyParameters ecpub = (ECPublicKeyParameters) key.getPublic();
        BigInteger privateKey = ecpriv.getD();
        ECPoint publicKey = ecpub.getQ();

//         System.out.println("公钥: " + Util.byteToHex(publicKey.getEncoded()));
//         System.out.println("私钥: " + Util.byteToHex(privateKey.toByteArray()));
    }

//    public static void main(String[] args){
//        generateKeyPair();
//    }

    //数据加密
    public static String encrypt(byte[] publicKey, byte[] data) {
        if (publicKey == null || publicKey.length == 0)
        {
            return null;
        }

        if (data == null || data.length == 0)
        {
            return null;
        }

        byte[] source = new byte[data.length];
        System.arraycopy(data, 0, source, 0, data.length);

        Cipher cipher = new Cipher ();
        SM2 sm2 = SM2.Instance();
        ECPoint userKey = sm2.ecc_curve.decodePoint(publicKey);

        ECPoint c1 = cipher.Init_enc(sm2, userKey);
        cipher.Encrypt(source);
        byte[] c3 = new byte[32];
        cipher.Dofinal(c3);

//      System.out.println("C1 " + Util.byteToHex(c1.getEncoded()));
//      System.out.println("C2 " + Util.byteToHex(source));
//      System.out.println("C3 " + Util.byteToHex(c3));
        //C1 C2 C3拼装成加密字串
        return Util.byteToHex(c1.getEncoded()) + Util.byteToHex(source) + Util.byteToHex(c3);

    }

    //数据解密
    public static byte[] decrypt(byte[] privateKey, byte[] encryptedData) {
        if (privateKey == null || privateKey.length == 0)
        {
            return null;
        }

        if (encryptedData == null || encryptedData.length == 0)
        {
            return null;
        }
        //加密字节数组转换为十六进制的字符串 长度变为encryptedData.length * 2
        String data = Util.byteToHex(encryptedData);
        /***分解加密字串
         * （C1 = C1标志位2位 + C1实体部分128位 = 130）
         * （C3 = C3实体部分64位  = 64）
         * （C2 = encryptedData.length * 2 - C1长度  - C2长度）
         */
        byte[] c1Bytes = Util.hexToByte(data.substring(0,130));
        int c2Len = encryptedData.length - 97;
        byte[] c2 = Util.hexToByte(data.substring(130,130 + 2 * c2Len));
        byte[] c3 = Util.hexToByte(data.substring(130 + 2 * c2Len,194 + 2 * c2Len));

        SM2 sm2 = SM2.Instance();
        BigInteger userD = new BigInteger (1, privateKey);

        //通过C1实体字节来生成ECPoint
        ECPoint c1 = sm2.ecc_curve.decodePoint(c1Bytes);
        Cipher cipher = new Cipher ();
        cipher.Init_dec(userD, c1);
        cipher.Decrypt(c2);
        cipher.Dofinal(c3);

        //返回解密结果
        return c2;
    }

    public static String encrypt(String data,String publicKey) {
        if (publicKey==null || publicKey.trim ().length ()==0){
            return data;
        }
        byte[] sourceData = data.getBytes();
        String cipherText = SM2Utils.encrypt(Util.hexToByte(publicKey), sourceData);
        return cipherText;
    }

//    public static void main(String[] args) throws Exception{
//        String public_key = "04D4B84FF14962E54B42135B45CC03396ABCF7B4AAC25CDD99CFA6F9338EE00017D69FABBCECAB505D00340A1DC952EDBEB71F1E85EBBD710E566658556381EED0";
//
//        String[] sourceData = {"6224243000000011",
//                "6212143000000000029",
//                "6212143000000000110",
//                "6212143000000000136",
//                "6212143000000000144",
//                "6212143000000000151",
//                "6212143000000000177",
//                "6212143000000000185",
//                "6226612345670000",
//                "6226612345670001",
//                "6226612345670002",
//                "6226612345670003",
//                "6226612345670004",
//                "6226612345670005",};
//
//        for (int i = 0; i < sourceData.length; i++){
//            byte[] source = sourceData[i].getBytes();
//            String cipherText = SM2Utils.encrypt(Util.hexToByte(public_key), source);
//            System.out.println("cipherText -> "+ cipherText);
//        }
//
//    }

//    public static void main(String[] args) throws Exception
//    {
//        //生成密钥对
//        generateKeyPair();
//
//        String plainText = "goodsadjksdjsad_7842141";
//        byte[] sourceData = plainText.getBytes();
//
//        //下面的秘钥可以使用generateKeyPair()生成的秘钥内容
//        // 国密规范正式私钥
//        String prik = "0BD031C85A84B02B2B653F753A8362C67DA4ECD8D116DD8182F823C7BF183456";
//        // 国密规范正式公钥
//        String pubk = "049E55CDA19CEF60FB42C923C8AD68F0EF02096314647788049A1986C1B9E144119DF948A3CAA1916C3BA5ABE804F174804D9DF076C6FAEF4056B30F743B08AB37";
//
//        System.out.println("加密: ");
//        String cipherText = SM2Utils.encrypt(Util.hexToByte(pubk), sourceData);
//        System.out.println(cipherText);
//        System.out.println("解密: ");
//        plainText = new String (SM2Utils.decrypt(Util.hexToByte(prik), Util.hexToByte(cipherText)));
//        System.out.println(plainText);
//    }

}