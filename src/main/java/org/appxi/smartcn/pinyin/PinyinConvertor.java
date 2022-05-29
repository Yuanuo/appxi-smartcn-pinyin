package org.appxi.smartcn.pinyin;

import org.appxi.smartcn.util.SmartCNHelper;
import org.appxi.smartcn.util.bytes.ByteArray;
import org.appxi.smartcn.util.bytes.BytesHelper;
import org.appxi.smartcn.util.dictionary.StringDictionary;
import org.appxi.smartcn.util.trie.AbstractDictionaryTrieApp;
import org.appxi.smartcn.util.trie.DoubleArrayTrieByAhoCorasick;
import org.appxi.util.FileHelper;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class PinyinConvertor extends AbstractDictionaryTrieApp<Pinyin[]> {
    public static final PinyinConvertor ONE = new PinyinConvertor();

    private PinyinConvertor() {
    }

    @Override
    protected final void loadDictionaries(DoubleArrayTrieByAhoCorasick<Pinyin[]> trie) {
        final List<Path> txtFiles = FileHelper.extractFiles(
                file -> getClass().getResourceAsStream(file),
                file -> SmartCNHelper.resolveData("pinyin").resolve(file),
                "data.txt");

        // load from bin
        final Path binFile = SmartCNHelper.resolveCache("pinyin/data.bin");
        if (!FileHelper.isTargetFileUpdatable(binFile, txtFiles.toArray(new Path[0]))) {
            final long st = System.currentTimeMillis();
            try {
                final ByteArray byteArray = BytesHelper.createByteArray(binFile);
                if (null != byteArray) {
                    final Pinyin[] valueEnums = Pinyin.values();
                    final int totalSize = byteArray.nextInt();
                    final Pinyin[][] valueArray = new Pinyin[totalSize][];
                    for (int i = 0; i < valueArray.length; ++i) {
                        final int itemSize = byteArray.nextInt();
                        valueArray[i] = new Pinyin[itemSize];
                        for (int j = 0; j < itemSize; ++j) {
                            valueArray[i][j] = valueEnums[byteArray.nextInt()];
                        }
                    }
                    trie.load(byteArray, valueArray);
                    return;
                }
            } finally {
                SmartCNHelper.logger.info("loadBin used after: " + (System.currentTimeMillis() - st));
            }
        }
        // load primary txt
        final TreeMap<String, Pinyin[]> primaryMap = new TreeMap<>();
        final Path fileTxt = SmartCNHelper.resolveData("pinyin/data.txt");
        if (FileHelper.exists(fileTxt)) {
            final StringDictionary dictionary = new StringDictionary("=");
            try (InputStream stream = Files.newInputStream(fileTxt)) {
                dictionary.load(stream);
            } catch (IOException ignore) {
            }
            dictionary.walkEntries((k, v) -> {
                final String[] tmpArr = v.split(",");
                final Pinyin[] valArr = new Pinyin[tmpArr.length];
                try {
                    for (int i = 0; i < tmpArr.length; ++i) {
                        valArr[i] = Pinyin.valueOf(tmpArr[i]);
                    }
                } catch (IllegalArgumentException e) {
                    SmartCNHelper.logger.warn("拼音词典" + fileTxt + "有问题在【" + k + "=" + v + "】", e);
                    return; // continue for next one
                }
                primaryMap.put(k, valArr);
            });
        }
        // build to trie
        long st = System.currentTimeMillis();
        trie.build(primaryMap);
        SmartCNHelper.logger.info("trie.build + " + (System.currentTimeMillis() - st));
        // save to bin
        FileHelper.makeParents(binFile);
        st = System.currentTimeMillis();
        if (trie.size() == primaryMap.size()) {
            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(binFile)))) {
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
        SmartCNHelper.logger.info("saveBin used after: " + (System.currentTimeMillis() - st));
    }

    public final List<Map.Entry<Character, Pinyin>> convert(String string) {
        return convert(string.toCharArray());
    }

    public final List<Map.Entry<Character, Pinyin>> convert(char... chars) {
        final Pinyin[][] wordNet = new Pinyin[chars.length][];
        getDictionaryTrie().parseText(chars, (begin, end, value) -> {
            int length = end - begin;
            if (wordNet[begin] == null || length > wordNet[begin].length) {
                wordNet[begin] = length == 1 ? new Pinyin[]{value[0]} : value;
            }
        });
        //
        final List<Map.Entry<Character, Pinyin>> result = new ArrayList<>(chars.length);
        for (int i = 0; i < wordNet.length; ) {
            if (wordNet[i] == null) {
                result.add(new AbstractMap.SimpleEntry<>(chars[i++], null));
                continue;
            }
            for (Pinyin itm : wordNet[i]) {
                result.add(new AbstractMap.SimpleEntry<>(chars[i++], itm));
            }
        }
        return result;
    }
}
