package net.xanarey;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {


        // TODO Ссылки "склеиваем" самостоятельно
        // https://www.avito.ru/moskva_i_mo?cd=1&p=1&q=apple+macbook+air+13+2020+intel
        // https://www.avito.ru/moskva_i_mo?cd=1&p=2&q=apple+macbook+air+13+2020+intel

        String startUrl = "https://www.avito.ru/moskva_i_mo?cd=1&p=";
        String finishUrl = "&q=apple+macbook+air+13+2020+intel";
        int currentPage = 1;

        Document doc = Jsoup.connect(startUrl + currentPage + finishUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3")
                .get();

        sleepThread();

        String totalAdsString = Objects.requireNonNull(doc.selectFirst("span.page-title-count-wQ7pG")).text();
        int intValueOnFirstPage = Integer.parseInt(totalAdsString.replaceAll("\\D+", ""));

        int currentCount = 0;
        int pageCount = 1;
        List<Integer> prices = new ArrayList<>();

        try {
            while (currentCount < intValueOnFirstPage) {
                String url = startUrl + currentPage + finishUrl;

                System.out.println("\n --------------------- Страница: [" + pageCount + "]  " + url);

                sleepThread();


                doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3")
                        .get();

                Elements elementsOfPage = doc.select("div.iva-item-body-KLUuy");
                if (elementsOfPage.isEmpty()) {
                    System.out.println("Не найдены товары по URL: " + url);
                    continue;
                }

                for (Element e : elementsOfPage) {
                    if (intValueOnFirstPage == currentCount) break;

                    Element urlElement = e.selectFirst("a[itemProp=url]");
                    assert urlElement != null;
                    String checkTitle = urlElement.attr("title");
                    if (checkTitle.contains("~~~~~~~~~~~~~~~~~~`")) {
                        currentCount++;
                        continue;
                    }

                    String currency = Objects.requireNonNull(e.selectFirst("meta[itemProp=priceCurrency]")).attr("content");
                    String priceString = Objects.requireNonNull(e.selectFirst("meta[itemProp=price]")).attr("content");

                    try {
                        int count = Integer.parseInt(priceString);
                        if (!(count < 40000 || count > 90000)) prices.add(count); // TODO Убираем доп. товары
                    } catch (NumberFormatException ex) {
                        System.out.println("Не удалось преобразовать цену в число: " + priceString);
                    }

                    String title = urlElement.attr("title");
                    System.out.println(title + ", Цена: " + priceString + " " + currency);

                    currentCount++;
                }

                calculate(prices, intValueOnFirstPage, pageCount, doc);

                currentPage++;
                pageCount++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void calculate(List<Integer> prices, int intValueOnFirstPage, int pageCount, Document doc) {
        System.out.println("\nСредняя цена: " + calculateAverage(prices));
        System.out.println("Исключено из подсчёта: " + (intValueOnFirstPage - prices.size()));
        System.out.println(intValueOnFirstPage + " позиций по выбранным условиям на " + (pageCount - 1) + " странице(ах)");
        System.out.println("\nПоиск осуществлен по запросу и выбранным областям: \n" + Objects.requireNonNull(doc.select("title").first()).text());
        System.out.println("Отобрано: " + prices.size());
        System.out.println("Самый дорогой - " + prices.stream().max(Integer::compareTo).orElse(null));
        System.out.println("Самый дешевый - " + prices.stream().min(Integer::compareTo).orElse(null));
    }

    private static double calculateAverage(List<Integer> prices) {
        return prices.isEmpty() ? 0 : prices.stream().mapToDouble(Integer::doubleValue).average().orElse(0);
    }

    private static void sleepThread() throws InterruptedException {
        double randomSleepTime = 0.5 + Math.random() * 0.5;
        long sleepMillis = (long) (randomSleepTime * 100);
        Thread.sleep(sleepMillis);
    }
}