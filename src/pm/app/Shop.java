/*
 * Copyright (C) 2021 Oscar
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
package pm.app;

import java.util.Locale;
import java.util.Scanner;

import pm.data.*;

/**
 *
 * @author Oscar
 */
public class Shop {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        ProductManager pm = ProductManager.getInstance();
        String option = "";
        Scanner sc = new Scanner(System.in);
        boolean pass = true;
        int id = 0;

        while (pass) {

            System.out.println("\n** PRODUCT MANAGER **\n"
                    + "1.- View products.\n"
                    + "2.- Search products.\n"
                    + "3.- Search reviews.\n"
                    + "4.- Add product.\n"
                    + "5.- Add review.\n"
                    + "6.- Delete product.\n"
                    + "0.- Exit.\n");

            option = sc.next();

            switch (option) {
                case "1":

                    pm.getProducts().stream().map(s -> pm.showProduct(s)).forEach(System.out::println);
                    break;

                case "2":
                    do {
                        System.out.println("You can search product by:\n"
                                + "1.- Id.\n"
                                + "2.- Name.\n");
                        String search = sc.next();

                        switch (search) {
                            case "1":
                                System.out.println("Enter the Id:\t");
                                id = sc.nextInt();
                                try {
                                    System.out.println(System.lineSeparator() + pm.findProduct(id));
                                } catch (ProductManagerException e) {
                                    System.out.println(e.getMessage());
                                }
                                pass = false;
                                break;
                            case "2":
                                System.out.println("Enter the Name:\t");
                                String name = sc.nextLine();
                                try {
                                    System.out.println(System.lineSeparator() + pm.findProduct(name));
                                } catch (ProductManagerException e) {
                                    System.out.println(e.getMessage());}
                                    pass = false;
                                    break; 

                            default:
                                System.out.println("Please enter a valid option ");
                                break;

                    }} while (pass);
                        break;

                      case "3":
                    System.out.println("Enter the id of the product you want to see reviews of:\t");
                    id = sc.nextInt();
                    for (Review r : pm.loadReviews(id)) {
                        System.out.println(r);
                    }
                    break;

                case "4":
                    StringBuilder productFormat = new StringBuilder();
                    System.out.println("Introduce your new product:\n"
                            + "\nName: ");
                    productFormat.append(sc.next());
                    System.out.println("\nType: (Drink o Food)");
                    productFormat.append(",");
                    productFormat.append(sc.next());
                    System.out.println("\nPrice: ");
                    productFormat.append(",");
                    productFormat.append(sc.next());
                    System.out.println("\nRating: (A number between 0 and 5)");
                    productFormat.append(",");
                    productFormat.append(sc.next());
                    System.out.println("\nBest Besfore (YYYY-MM-dd): ");
                    productFormat.append(",");
                    productFormat.append(sc.next());
                    System.out.println(productFormat.toString());
                    Product p = pm.parseProduct(productFormat.toString());
                    pm.printProductReport(p.getId());
                    break;

                case "5":
                    StringBuilder productReview = new StringBuilder();
                    System.out.println("Introduce your new review:\n"
                            + "Id:\t");
                    id = sc.nextInt();
                    System.out.println("\nStars: (A number between 0 and 5)");
                    productReview.append(",");
                    productReview.append(sc.next());
                    System.out.println("\nComments: ");
                    productReview.append(",");
                    productReview.append(sc.next());
                    System.out.println(productReview.toString());
                    pm.parseReview(productReview.toString());
                    pm.printProductReport(id);
                    break;

                case "6":
                    System.out.println("You can search product by:\n"
                            + "1.- Id.\n"
                            + "2.- Name.\n");
                    int delete = sc.nextInt();

                    switch (delete) {
                        case 1:
                            System.out.println("Enter the Id:\t");
                            id = sc.nextInt();
                            System.out.println(pm.deleteProduct(id));
                            break;
                        case 2:
                            System.out.println("Enter the Name:\t");
                            String name = sc.nextLine();
                            System.out.println(pm.deleteProduct(name));
                            break;
                        case 0:
                            break;
                        default:
                            System.out.println("Please enter a valid option ");
                            break;

                    }
                    break;

                case "0":
                    System.out.println("Coming back soon");
                    pass = false;
                    break;

                default:
                    System.out.println("Introduce a valid number");
                    break;
            }
        
            
            }
        }
    }
