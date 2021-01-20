package org.appxi.hanlp.pinyin;

import org.appxi.hanlp.util.HanlpHelper;
import org.appxi.hanlp.util.bytes.ByteArray;
import org.appxi.hanlp.util.bytes.BytesHelper;
import org.appxi.hanlp.util.dictionary.StringDictionary;
import org.appxi.hanlp.util.trie.AbstractDictionaryTrieApp;
import org.appxi.hanlp.util.trie.DoubleArrayTrieByAhoCorasick;
import org.appxi.util.FileHelper;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;


public class PinyinConvertor extends AbstractDictionaryTrieApp<Pinyin[]> {
    private static final String pathTxt = "/appxi/hanlpPinyin/data.txt";
    private static final String pathBin = pathTxt.replace(".txt", ".bin").substring(1);

    public static final PinyinConvertor instance = new PinyinConvertor();
    public static final Pinyin[] pinyins = Pinyin.values();

    private PinyinConvertor() {
    }

    @Override
    protected final void loadDictionaries(DoubleArrayTrieByAhoCorasick<Pinyin[]> trie) {
        final List<Path> fileTxts = HanlpHelper.ensureFilesExtracted(v -> getClass().getResourceAsStream(v), pathTxt);

        // load from bin
        final Path fileBin = HanlpHelper.resolveCache(pathBin);
        if (!FileHelper.isTargetFileUpdatable(fileBin, fileTxts.toArray(new Path[0]))) {
            final long st = System.currentTimeMillis();
            try {
                final ByteArray byteArray = BytesHelper.createByteArray(fileBin);
                if (null != byteArray) {
                    final int totalSize = byteArray.nextInt();
                    final Pinyin[][] valueArray = new Pinyin[totalSize][];
                    for (int i = 0; i < valueArray.length; ++i) {
                        final int itemSize = byteArray.nextInt();
                        valueArray[i] = new Pinyin[itemSize];
                        for (int j = 0; j < itemSize; ++j) {
                            valueArray[i][j] = pinyins[byteArray.nextInt()];
                        }
                    }
                    trie.load(byteArray, valueArray);
                    return;
                }
            } finally {
                HanlpHelper.LOG.info("loadBin used times: " + (System.currentTimeMillis() - st));
            }
        }
        // load primary txt
        final TreeMap<String, Pinyin[]> primaryMap = new TreeMap<>();
        final Path fileTxt = HanlpHelper.resolveData(pathTxt);
        if (FileHelper.exists(fileTxt)) {
            final StringDictionary dictionary = new StringDictionary("=");
            dictionary.load(HanlpHelper.ensureStream(fileTxt));
            dictionary.walkEntries((k, v) -> {
                final String[] tmpArr = v.split(",");
                final Pinyin[] valArr = new Pinyin[tmpArr.length];
                try {
                    for (int i = 0; i < tmpArr.length; ++i) {
                        valArr[i] = Pinyin.valueOf(tmpArr[i]);
                    }
                } catch (IllegalArgumentException e) {
                    HanlpHelper.LOG.severe("拼音词典" + fileTxt + "有问题在【" + k + "=" + v + "】，e=" + e);
                    return; // continue for next one
                }
                primaryMap.put(k, valArr);
            });
        }
        // build to trie
        long st = System.currentTimeMillis();
        trie.build(primaryMap);
        HanlpHelper.LOG.info("trie.build + " + (System.currentTimeMillis() - st));
        // save to bin
        FileHelper.makeParents(fileBin);
        st = System.currentTimeMillis();
        if (trie.size() == primaryMap.size()) {
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(fileBin)))) {
                out.writeInt(primaryMap.size());
                Pinyin[] value;
                for (Map.Entry<String, Pinyin[]> entry : primaryMap.entrySet()) {
                    value = entry.getValue();
                    out.writeInt(value.length);
                    for (Pinyin pinyin : value) {
                        out.writeInt(pinyin.ordinal());
                    }
                }
                trie.save(out);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        HanlpHelper.LOG.info("saveBin used times: " + (System.currentTimeMillis() - st));
    }

    public final List<Pinyin> convert(String string) {
        return convert(true, string.toCharArray());
    }

    public final List<Pinyin> convert(String string, boolean remainNone) {
        return convert(remainNone, string.toCharArray());
    }

    public final List<Pinyin> convert(char... chars) {
        return convert(true, chars);
    }

    public final List<Pinyin> convert(boolean remainNone, char... chars) {
        final Pinyin[][] wordNet = new Pinyin[chars.length][];
        getDictionaryTrie().parseText(chars, (begin, end, value) -> {
            int length = end - begin;
            if (wordNet[begin] == null || length > wordNet[begin].length) {
                wordNet[begin] = length == 1 ? new Pinyin[]{value[0]} : value;
            }
        });
        final List<Pinyin> result = new ArrayList<>(chars.length);
        for (int offset = 0; offset < wordNet.length; ) {
            if (wordNet[offset] == null) {
                if (remainNone) result.add(Pinyin.none5);
                ++offset;
                continue;
            }
            Collections.addAll(result, wordNet[offset]);
            offset += wordNet[offset].length;
        }
        return result;
    }
}
