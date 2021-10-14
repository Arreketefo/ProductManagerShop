/*
 * Copyright (C) 2021 Oscar Aguilar
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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
 */
package pm.data;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author Oscar Aguilar
 */
public class ProductManager {

    private static Set<Product> products = new TreeSet<>();
    private ResourceFormatter formatter = new ResourceFormatter(Locale.UK);
    private final ResourceBundle config = ResourceBundle.getBundle("pm.data.config");
    private final MessageFormat reviewFormat = new MessageFormat(config.getString("review.data.format"));
    private final MessageFormat productFormat = new MessageFormat(config.getString("product.data.format"));

    private static final Map<String, ResourceFormatter> formatters = Map.of("en_GB", new ResourceFormatter(Locale.UK),
            "en_US", new ResourceFormatter(Locale.US), "fr_FR", new ResourceFormatter(Locale.FRANCE), "es_ES", new ResourceFormatter(new Locale("es", "ES")),
            "de_GE", new ResourceFormatter(Locale.GERMANY));

    private static final ProductManager pm = new ProductManager();

    private static final Logger logger = Logger.getLogger(ProductManager.class.getName());

    private final Path dataFolder = Path.of(config.getString("data.folder"));
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock writeLock = lock.writeLock();
    private final Lock readLock = lock.readLock();

    private static class ResourceFormatter {

        private final Locale locale;
        private final ResourceBundle resourcesPrint;
        private final ResourceBundle resourcesLoad;
        private final DateTimeFormatter dateFormatLoad;
        private final DateTimeFormatter dateFormatPrint;
        private final NumberFormat moneyFormatLoad;
        private final NumberFormat moneyFormatPrint;

        private ResourceFormatter(Locale locale) {
            this.locale = locale;
            this.resourcesPrint = ResourceBundle.getBundle("pm.data.resources");
            this.resourcesLoad = ResourceBundle.getBundle("pm.data.resources", locale);
            this.dateFormatLoad = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).localizedBy(locale);
            this.dateFormatPrint = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            this.moneyFormatLoad = NumberFormat.getCurrencyInstance(locale);
            this.moneyFormatPrint = NumberFormat.getCurrencyInstance(Locale.ENGLISH);

        }

        private String formatProductPrint(Product product) {
            return MessageFormat.format(resourcesPrint.getString("product"), product.getName(), product.getType(),
                    moneyFormatPrint.format(product.getPrice()), product.getRating().getStars(),
                    dateFormatPrint.format(product.getBestBefore()));
        }

        private String formatProductLoad(Product product) {
            return MessageFormat.format(resourcesLoad.getString("product"), product.getName(), product.getType(),
                    moneyFormatLoad.format(product.getPrice()), product.getRating().getStars(),
                    dateFormatLoad.format(product.getBestBefore()), product.printReviews());
        }

        private String formatReviewPrint(Review review) {
            return MessageFormat.format(resourcesPrint.getString("review"), review.getRating().getStars(),
                    review.getComments());
        }

        private String getText(String key) {
            return resourcesLoad.getString(key);
        }

    }

    public static ProductManager getInstance() {
        loadAllData();
        return pm;
    }

    public Set<Product> getProducts() {
        return products;
    }

    public String showProduct(Product product) {

        return formatter.formatProductLoad(product);
    }
    public String showProduct(Product product, String lang) {

        return formatters.getOrDefault(lang, formatter).formatProductLoad(product);
    }

    public static Set<String> getSupportedLocales() {
        return formatters.keySet();
    }

    public Product createProductFood(String type, String name, BigDecimal price, Rating rating,
            LocalDate bestBefore) {
        Product product = null;
        try {
            writeLock.lock();
            product = new Food(type, name, price, rating, bestBefore);
            products.add(product);
        } catch (Exception ex) {
            logger.log(Level.INFO, "Error adding products {0}", ex.getMessage());
        } finally {
            writeLock.unlock();
        }

        return product;
    }

    public Product createProductDrink(String type, String name, BigDecimal price, Rating rating) {
        Product product = null;
        try {
            writeLock.lock();
            product = new Drink(type, name, price, rating);
            products.add(product);
        } catch (Exception ex) {
            logger.log(Level.INFO, "Error adding products {0}", ex.getMessage());
        } finally {
            writeLock.unlock();
        }

        return product;
    }

    public Product findProduct(int id) throws ProductManagerException {
        try {
            readLock.lock();
            return products.stream().filter(p -> p.getId() == id).findFirst()
                    .orElseThrow(() -> new ProductManagerException("Product with id " + id + " not found"));
        } finally {
            readLock.unlock();
        }

    }

    public Product findProduct(String name) throws ProductManagerException {
        try {
            readLock.lock();
            return products.stream().filter(p -> p.getName().equals(name)).findFirst()
                    .orElseThrow(() -> new ProductManagerException("Product with name " + name + " not found"));
        } finally {
            readLock.unlock();
        }

    }

    public Product reviewProduct(int id, int stars, String comments) {
        try {
            return reviewProduct(findProduct(id), Rateable.convertInt(stars), comments);
        } catch (ProductManagerException ex) {
            logger.log(Level.INFO, ex.getMessage());
        }
        return null;
    }

    public Product reviewProduct(int id, Rating rating, String comments) {
        try {
            return reviewProduct(findProduct(id), rating, comments);
        } catch (ProductManagerException ex) {
            logger.log(Level.INFO, ex.getMessage());
        }
        return null;
    }

    public Product reviewProduct(Product product, Rating rating, String comments) {

        Set<Review> reviews = product.getReviews();
        reviews.add(new Review(rating, comments));

        product = product.applyRating(Rateable.convertInt(
                (int) Math.round(reviews.stream().mapToInt(r -> r.getRating().ordinal()).average().orElse(0))));

        products.add(product);
        return product;
    }

    public void printProductReport(int id) {
        try {
            printProductReport(findProduct(id));
        } catch (ProductManagerException ex) {
            logger.log(Level.INFO, ex.getMessage());
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "Error printing product report", ex);
        }
    }

    public void printProductReport(Product product) throws IOException {
        Set<Review> reviews = product.getReviews();
        String form = formatter.formatProductPrint(product);

        Path productFile = dataFolder
                .resolve(MessageFormat.format(config.getString("product.data.file"), product.getId()));

        try (BufferedWriter out = Files.newBufferedWriter(productFile, StandardOpenOption.CREATE);
                Stream<String> data = Files.lines(productFile)) {

            if (data.noneMatch(s -> s.equals(form))) {
                out.append((form) + System.lineSeparator());
                if (reviews.isEmpty()) {
                    out.append(formatter.getText("no.review") + System.lineSeparator());
                } else {
                    out.append(reviews.stream().map(r -> formatter.formatReviewPrint(r) + System.lineSeparator())
                            .collect(Collectors.joining()));
                }
                out.flush();
            }

        }

    }

    public void printProducts(Predicate<Product> filter, Comparator<Product> sorter, String languageTag) {

        formatter = formatters.getOrDefault(languageTag, formatter);
        StringBuilder txt = new StringBuilder();
        products.stream().filter(filter).sorted(sorter).forEach(p -> txt.append(formatter.formatProductLoad(p)).append("\n"));

        System.out.println(txt);
    }

    public void printReview(int id, Review review) {
        try {
            Product product = findProduct(id);
            products.removeIf(s -> s.getId() == id);
            product.setReview(review);
            products.add(product);
        } catch (ProductManagerException ex) {
            logger.log(Level.INFO, "The product for which you want to find the review is not found {0}", ex.getMessage());
        }
    }

    private static void loadAllData() {

        Path path = pm.dataFolder;
        if (Files.notExists(path)) {
            try {
                Files.createDirectory(path);
            } catch (IOException ex) {
                Logger.getLogger(ProductManager.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try (Stream<Path> data = Files.list(pm.dataFolder)) {

            products = data.map(pm::loadProductCsv).filter(s -> s != null)
                    .collect(Collectors.toSet());
        } catch (IOException ex) {
            logger.log(Level.WARNING, "Error loading all data {0}", ex.getMessage());
            ex.printStackTrace();
        }
    }

    public Product loadProductCsv(Path file) {

        Product product = null;
        Path path = dataFolder.resolve(file);

        try (Stream<String> data = Files.lines(path)) {

            String[] newProduct = data.findFirst().orElseThrow().split(",");

            String name = newProduct[0].substring(6);
            String type = newProduct[1].substring(7);

            BigDecimal price = BigDecimal.valueOf(Double.parseDouble(newProduct[2].substring(9)));
            Rating rating = Rateable.convertString(newProduct[3].substring(9));
            Set<Review> reviewsLoad = Files.readAllLines(path).stream().skip(1).filter(s -> !s.equals("Not reviewed")).map((String r) -> {
                String[] a = r.split("\t");
                if (a.length == 2) {
                    return new Review(Rateable.convertString(a[0].substring(8)), a[1]);
                } else {
                    return new Review(Rateable.DEFAULT_RATING, formatter.getText("no.review"));
                }
            }).collect(Collectors.toSet());
            if (type.equals("Drink")) {
                product = new Drink(type, name, price, rating, reviewsLoad);
            } else if (type.equals("Food")) {

                LocalDate bestBefore = LocalDate.parse(newProduct[4].substring(14));
                product = new Food(type, name, price, rating, bestBefore, reviewsLoad);

            }

        } catch (IOException ex) {
            logger.log(Level.WARNING, "Error loadind product", ex);
        }
        return product;
    }

    public Set<Review> loadReviews(int id) {
        return products.stream().filter(p -> p.getId() == id).map(Product::getReviews)
                .flatMap(s -> s.stream()).collect(Collectors.toSet());

    }

    public String deleteProduct(int id) {
        if (products.removeIf(s -> s.getId() == id)) {
            try {
                Files.delete(dataFolder.resolve(MessageFormat.format(config.getString("product.data.file"), id)));
            } catch (IOException e) {
                System.out.println("problem deleting product ");
            }
            return "Product with id " + id + " has been removed";
        } else {
            return "the product could not be removed";
        }
    }

    public String deleteProduct(String name) {
        int id = 0;
        try {
            id = findProduct(name).getId();
        } catch (ProductManagerException e) {
            System.out.println("Product cannot be found");
        }
        return deleteProduct(id);
    }

    public Review parseReview(String text) {
        Review review = null;
        try {
            Object[] values = reviewFormat.parse(text);
            review = new Review(Rateable.convertString((String) values[0]), (String) values[1]);
        } catch (ParseException | NumberFormatException ex) {
            logger.log(Level.WARNING, "Error parsing review {0}", text);
        }
        return review;
    }

    public Product parseProduct(String text) {
        Product product = null;
        Object[] values = null;
        try {
            values = productFormat.parse(text);
            if (values != null) {
                String name = (String) values[0];
                String type = values[1].toString();
                BigDecimal price = BigDecimal.valueOf(Double.parseDouble((String) values[2]));
                Rating rating = Rateable.convertString((String) values[3]);
                if (type.equals("Drink")) {
                    product = createProductDrink(type, name, price, rating);
                } else if (type.equals("Food")) {
                    LocalDate bestBefore = LocalDate.parse((String) values[4]);
                    product = createProductFood(type, name, price, rating, bestBefore);
                }
            }
            products.add(product);
            printProductReport(product);

        } catch (ParseException | IOException ex) {
            Logger.getLogger(ProductManager.class.getName()).log(Level.SEVERE, "problema al dar formato", ex);
        }
        return product;
    }

    public Map<String, String> getDiscounts(String languageTag) {
        formatter = formatters.getOrDefault(languageTag, formatter);
        return products.stream()
                .collect(Collectors.groupingBy(product -> product.getRating().getStars(),
                        Collectors.collectingAndThen(
                                Collectors.summingDouble(product -> product.getDiscount().doubleValue()),
                                discount -> formatter.moneyFormatLoad.format(discount))));

    }
}
