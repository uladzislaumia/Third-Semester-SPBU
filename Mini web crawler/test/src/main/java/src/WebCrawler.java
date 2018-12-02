package src;

import javafx.util.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import src.concurrent.CustomConcurrentLinkedQueue;
import src.concurrent.CustomConcurrentSkipListSet;
import src.concurrent.CustomThreadExecutor;

import java.io.*;
import java.net.URL;
import java.util.concurrent.*;

public class WebCrawler {

    private class SearchAndDownloadTask implements Runnable {
        final String folderName = "/home/vladislav/Downloads/Pages/";

        @Override
        public void run() {
            Pair<String, Integer> page = notVisitedPages.poll();
            if (page != null) {
                try {
                    String url = page.getKey();
                    int depth = page.getValue();
                    Document doc = Jsoup.connect(url).get();

                    if (depth < maxDepth) {
                        Elements links = doc.select("a[href]");
                        for (Element link : links) {
                            String pageURL = link.attr("abs:href");
                            if (allPages.add(pageURL)) {
                                notVisitedPages.add(new Pair<>(pageURL, depth + 1));
                                executorService.execute(new SearchAndDownloadTask());
                            }
                        }
                    }

                    System.out.println(url + ";");
                    int indexName = url.lastIndexOf("/");
                    if (indexName == url.length() - 1) {
                        indexName = url.substring(0, indexName).lastIndexOf("/");
                    }
                    String name = url.substring(indexName, url.length());

                    InputStream in = new URL(url).openStream();
                    OutputStream out = new BufferedOutputStream(new FileOutputStream(folderName + name));
                    for (int b; (b = in.read()) != -1; ) {
                            out.write(b);
                    }
                    out.close();
                    in.close();

                    downloadedPagesNumber++;
                    if (downloadedPagesNumber == allPages.size()) {
                        executorService.shutdown();
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public WebCrawler(int threadsNumber) {
        downloadedPagesNumber = 0;
        allPages = new CustomConcurrentSkipListSet<>();
        notVisitedPages = new CustomConcurrentLinkedQueue<>();
//        executorService = Executors.newFixedThreadPool(threadsNumber);
        executorService = new CustomThreadExecutor(threadsNumber);
    }

    public void crawl(String mainURL, int maxDepth) {
        this.mainURL = mainURL;
        this.maxDepth = maxDepth;
        notVisitedPages.add(new Pair<>(this.mainURL, 1));
        allPages.add(this.mainURL);
        executorService.execute(new SearchAndDownloadTask());
    }

    private String mainURL;
    private int maxDepth;
    private CustomConcurrentLinkedQueue<Pair<String, Integer>> notVisitedPages;
    private CustomConcurrentSkipListSet<String> allPages;
    private ExecutorService executorService;
    private volatile int downloadedPagesNumber;
}