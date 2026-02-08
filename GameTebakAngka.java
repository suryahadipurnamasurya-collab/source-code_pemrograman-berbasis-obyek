import java.util.Scanner;
import java.util.Random;

public class GameTebakAngka {
    public static void main(String[] args) {
        try (Scanner input = new Scanner(System.in)) {
            Random random = new Random();
            
            int angkaRahasia = random.nextInt(100) + 1; // Angka 1 sampai 100
            int tebakan;
            int jumlahPercobaan = 0;
            boolean menang = false;

            System.out.println("Selamat Datang di Game Tebak Angka!");
            System.out.println("Saya sudah memikirkan angka antara 1 sampai 100.");

            while (!menang) {
                System.out.print("Masukkan tebakanmu: ");
                tebakan = input.nextInt();
                jumlahPercobaan++;

                if (tebakan == angkaRahasia) {
                    menang = true;
                } else if (tebakan < angkaRahasia) {
                    System.out.println("Terlalu rendah! Coba lagi.");
                } else {
                    System.out.println("Terlalu tinggi! Coba lagi.");
                }
            }

            System.out.println("Selamat! Kamu berhasil menebak angka " + angkaRahasia);
            System.out.println("Jumlah percobaan: " + jumlahPercobaan);
        }
    }
}