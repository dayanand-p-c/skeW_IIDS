package com.dis.service;

import com.dis.config.ClockConfig;
import com.dis.model.*;
import com.dis.model.enums.*;
import com.dis.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Profile({"seed", "dev"})
public class SyntheticDataSeederService implements CommandLineRunner {

    private final DrugMasterRepository drugRepository;
    private final SupplierRepository supplierRepository;
    private final BatchRepository batchRepository;
    private final TransactionRepository transactionRepository;
    private final SystemConfigRepository configRepository;
    private final AppUserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final RiskEvaluationService riskEvaluationService;
    private final Clock clock;

    @Value("${app.seed.random-seed:42}")
    private long seed;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Override
    public void run(String... args) {
        if (!seedEnabled) {
            log.info("Synthetic data seeding is disabled by config.");
            return;
        }

        if (drugRepository.count() >= 40 && batchRepository.count() > 0) {
            log.info("Database already seeded with {} drugs and {} batches. Skipping re-seed.", drugRepository.count(), batchRepository.count());
            return;
        }

        log.info("Starting 12-month synthetic data generator with fixed seed {}...", seed);
        seedData();
        log.info("Synthetic data seeding successfully completed!");
    }

    @Transactional
    public void seedData() {
        Random rand = new Random(seed);
        LocalDate simulatedToday = LocalDate.now(clock);
        LocalDate startOfSim = simulatedToday.minusDays(365);

        // 0. Seed configs if missing
        if (configRepository.count() == 0) {
            String[][] defaultConfigs = {
                    {"weight.stock", "0.4"},
                    {"weight.expiry", "0.4"},
                    {"weight.variability", "0.2"},
                    {"expiry.warn.days", "90"},
                    {"expiry.critical.days", "30"},
                    {"spike.ratio", "1.5"},
                    {"band.watch", "30"},
                    {"band.critical", "60"},
                    {"route.LOW_STOCK", "PHARMACIST,ADMIN"},
                    {"route.NEAR_EXPIRY", "PHARMACIST,WAREHOUSE"},
                    {"route.HIGH_RISK", "ADMIN,PHARMACIST"}
            };
            for (String[] cfg : defaultConfigs) {
                configRepository.save(new SystemConfig(cfg[0], cfg[1]));
            }
        }

        // 0.1 Seed users if missing
        if (userRepository.count() == 0) {
            userRepository.save(AppUser.builder()
                    .username("admin")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .email("admin@hospital.org")
                    .role(UserRole.ADMIN)
                    .active(true)
                    .build());
            userRepository.save(AppUser.builder()
                    .username("pharmacist")
                    .passwordHash(passwordEncoder.encode("pharma123"))
                    .email("pharmacist@hospital.org")
                    .role(UserRole.PHARMACIST)
                    .active(true)
                    .build());
            userRepository.save(AppUser.builder()
                    .username("warehouse")
                    .passwordHash(passwordEncoder.encode("warehouse123"))
                    .email("warehouse@hospital.org")
                    .role(UserRole.WAREHOUSE)
                    .active(true)
                    .build());
        }

        // 1. Seed 8 Suppliers with lead times 2 to 14 days
        List<Supplier> suppliers = new ArrayList<>();
        String[][] supDefs = {
                {"SUP-MED-01", "MediSource Global", "orders@medisource.com", "3"},
                {"SUP-BIO-02", "BioTech Logistics", "supply@biotech.org", "5"},
                {"SUP-APX-03", "Apex Pharmaceuticals", "contact@apexpharma.com", "7"},
                {"SUP-NOV-04", "NovaHealth Care", "sales@novahealth.com", "2"},
                {"SUP-ZEN-05", "Zenith Pharma Ltd", "distrib@zenith.com", "10"},
                {"SUP-CLF-06", "Crestline Remedies", "orders@crestline.com", "14"},
                {"SUP-SLV-07", "SilverLine Health", "support@silverline.com", "4"},
                {"SUP-PRM-08", "PrimeRx Logistics", "logistics@primerx.com", "8"}
        };

        for (String[] def : supDefs) {
            Supplier s = supplierRepository.findById(def[0]).orElse(null);
            if (s == null) {
                s = supplierRepository.save(Supplier.builder()
                        .supplierId(def[0])
                        .name(def[1])
                        .contact(def[2])
                        .leadTimeDays(Integer.parseInt(def[3]))
                        .build());
            }
            suppliers.add(s);
        }

        // 2. Seed ~50 Drugs in 8 Categories
        String[][] drugCatalog = {
                // Antibiotics
                {"AMOX-500", "Amoxicillin 500mg", "antibiotic", "capsule", "12.50", "0.95"},
                {"CIPRO-500", "Ciprofloxacin 500mg", "antibiotic", "tablet", "18.00", "0.95"},
                {"AZITH-250", "Azithromycin 250mg", "antibiotic", "tablet", "24.00", "0.95"},
                {"DOXY-100", "Doxycycline 100mg", "antibiotic", "capsule", "14.20", "0.90"},
                {"CEPH-500", "Cephalexin 500mg", "antibiotic", "capsule", "16.50", "0.95"},
                {"AUGM-625", "Augmentin 625mg", "antibiotic", "tablet", "32.00", "0.99"},

                // Analgesics
                {"PARA-500", "Paracetamol 500mg", "analgesic", "tablet", "4.50", "0.95"},
                {"IBU-400", "Ibuprofen 400mg", "analgesic", "tablet", "6.20", "0.95"},
                {"TRAM-50", "Tramadol 50mg", "analgesic", "capsule", "22.00", "0.99"},
                {"ASP-100", "Aspirin 100mg", "analgesic", "tablet", "3.80", "0.90"},
                {"DICLO-50", "Diclofenac Sodium 50mg", "analgesic", "tablet", "8.50", "0.95"},
                {"KETOR-10", "Ketorolac 10mg", "analgesic", "tablet", "19.00", "0.95"},

                // Cardiac
                {"ATOR-20", "Atorvastatin 20mg", "cardiac", "tablet", "15.00", "0.95"},
                {"AMLOD-5", "Amlodipine 5mg", "cardiac", "tablet", "7.50", "0.95"},
                {"METOP-50", "Metoprolol 50mg", "cardiac", "tablet", "11.20", "0.95"},
                {"LOSAR-50", "Losartan 50mg", "cardiac", "tablet", "13.40", "0.95"},
                {"CLOP-75", "Clopidogrel 75mg", "cardiac", "tablet", "28.00", "0.99"},
                {"DIGOX-025", "Digoxin 0.25mg", "cardiac", "tablet", "21.00", "0.99"},

                // Antidiabetic
                {"METF-500", "Metformin 500mg", "antidiabetic", "tablet", "5.00", "0.95"},
                {"GLIM-2", "Glimepiride 2mg", "antidiabetic", "tablet", "9.80", "0.95"},
                {"INS-REG", "Regular Insulin 100IU/ml", "antidiabetic", "vial", "85.00", "0.99"},
                {"DAPA-10", "Dapagliflozin 10mg", "antidiabetic", "tablet", "45.00", "0.95"},
                {"SITA-100", "Sitagliptin 100mg", "antidiabetic", "tablet", "52.00", "0.95"},
                {"EMP-25", "Empagliflozin 25mg", "antidiabetic", "tablet", "60.00", "0.95"},

                // Respiratory
                {"SALB-100", "Salbutamol Inhaler 100mcg", "respiratory", "inhaler", "38.00", "0.99"},
                {"BUD-200", "Budesonide Inhaler 200mcg", "respiratory", "inhaler", "55.00", "0.95"},
                {"MONT-10", "Montelukast 10mg", "respiratory", "tablet", "14.50", "0.90"},
                {"LEVO-5", "Levocetirizine 5mg", "respiratory", "tablet", "5.50", "0.90"},
                {"THEO-300", "Theophylline 300mg", "respiratory", "tablet", "12.00", "0.95"},
                {"TIOT-18", "Tiotropium 18mcg", "respiratory", "rotacap", "75.00", "0.95"},

                // Gastro
                {"OMEP-20", "Omeprazole 20mg", "gastro", "capsule", "6.50", "0.95"},
                {"PANT-40", "Pantoprazole 40mg", "gastro", "tablet", "8.90", "0.95"},
                {"RANI-150", "Ranitidine 150mg", "gastro", "tablet", "4.00", "0.90"},
                {"ONDA-4", "Ondansetron 4mg", "gastro", "tablet", "12.50", "0.95"},
                {"DOMP-10", "Domperidone 10mg", "gastro", "tablet", "5.20", "0.90"},
                {"SUCR-1000", "Sucralfate 1000mg", "gastro", "suspension", "22.00", "0.95"},

                // Vitamin & Supplements
                {"VIT-C-500", "Vitamin C 500mg", "vitamin", "chewable", "3.00", "0.90"},
                {"VIT-D3-60K", "Cholecalciferol 60,000 IU", "vitamin", "capsule", "18.00", "0.90"},
                {"B-COMP", "Vitamin B-Complex Forte", "vitamin", "tablet", "4.20", "0.90"},
                {"CAL-500", "Calcium Carbonate 500mg", "vitamin", "tablet", "6.00", "0.90"},
                {"FOLIC-5", "Folic Acid 5mg", "vitamin", "tablet", "2.50", "0.90"},
                {"ZINC-50", "Zinc Sulphate 50mg", "vitamin", "tablet", "3.50", "0.90"},

                // Antiseptic & Wound Care
                {"POVI-100", "Povidone Iodine 10% Ointment", "antiseptic", "tube", "16.00", "0.95"},
                {"CHLOR-05", "Chlorhexidine 0.5% Solution", "antiseptic", "bottle", "28.00", "0.95"},
                {"SILV-1", "Silver Sulfadiazine 1% Cream", "antiseptic", "tube", "35.00", "0.95"},
                {"HYDRO-3", "Hydrogen Peroxide 3%", "antiseptic", "bottle", "12.00", "0.90"},
                {"ALCO-70", "Isopropyl Alcohol 70% Spray", "antiseptic", "bottle", "14.00", "0.95"},
                {"BACT-20", "Bacitracin Ointment 20g", "antiseptic", "tube", "22.00", "0.90"}
        };

        List<DrugMaster> drugs = new ArrayList<>();
        for (String[] def : drugCatalog) {
            DrugMaster d = drugRepository.findById(def[0]).orElse(null);
            if (d == null) {
                d = drugRepository.save(DrugMaster.builder()
                        .drugId(def[0])
                        .name(def[1])
                        .category(def[2])
                        .unit(def[3])
                        .unitCost(new BigDecimal(def[4]))
                        .serviceLevel(new BigDecimal(def[5]))
                        .build());
            }
            drugs.add(d);
        }

        // 3. Generate 12 months of daily demand and replenish with fixed policy
        PrintWriter writer = null;
        try {
            writer = new PrintWriter(new FileWriter("generated_demand.csv"));
            writer.println("date,drug_id,unmet_demand,dispensed_demand,ground_truth_demand");
        } catch (Exception e) {
            log.warn("Could not create generated_demand.csv ({}): proceeding with database seeding only.", e.getMessage());
        }

        try {

            // Base rates per drug
            Map<String, Double> baseRates = new HashMap<>();
            for (DrugMaster drug : drugs) {
                baseRates.put(drug.getDrugId(), 10.0 + rand.nextDouble() * 30.0);
            }

            // Track active inventory per batch in memory during simulation
            Map<String, Batch> simBatches = new HashMap<>();
            int batchSeq = 1;

            for (int dayOffset = 365; dayOffset >= 0; dayOffset--) {
                LocalDate curDate = simulatedToday.minusDays(dayOffset);
                int dayOfWeek = curDate.getDayOfWeek().getValue();
                double weekdayFactor = (dayOfWeek == 6 || dayOfWeek == 7) ? 0.6 : 1.1; // lower weekends
                double seasonFactor = 1.0 + 0.3 * Math.sin(2 * Math.PI * (curDate.getDayOfYear()) / 365.0);

                for (DrugMaster drug : drugs) {
                    double base = baseRates.get(drug.getDrugId());
                    double expected = base * seasonFactor * weekdayFactor;
                    // Poisson / Gaussian approximation noise
                    double noise = (rand.nextGaussian() * Math.sqrt(expected));
                    int groundTruth = Math.max(0, (int) Math.round(expected + noise));

                    // Occasional spikes (approx 3 per year)
                    if (rand.nextDouble() < 0.008) {
                        groundTruth = (int) Math.round(groundTruth * (2.0 + rand.nextDouble() * 1.5));
                    }

                    // Compute current active stock in sim
                    long curStock = simBatches.values().stream()
                            .filter(b -> b.getDrug().getDrugId().equals(drug.getDrugId())
                                    && b.getStatus() == BatchStatus.ACTIVE
                                    && b.getExpiryDate().isAfter(curDate))
                            .mapToLong(Batch::getQuantity)
                            .sum();

                    // Replenishment trigger (naive fixed policy: when stock < 300, order 600)
                    if (curStock < 300 && rand.nextDouble() < 0.25) {
                        Supplier sup = suppliers.get(rand.nextInt(suppliers.size()));
                        int shelfMonths = 12 + rand.nextInt(25); // 12 to 36 months
                        LocalDate mfg = curDate.minusDays(rand.nextInt(30));
                        LocalDate exp = mfg.plusMonths(shelfMonths);

                        String bId = String.format("BT-%s-%02d%02d-%03d",
                                drug.getDrugId(),
                                mfg.getYear() % 100,
                                mfg.getMonthValue(),
                                batchSeq++);

                        int inboundQty = 400 + rand.nextInt(400);

                        Batch b = Batch.builder()
                                .batchId(bId)
                                .drug(drug)
                                .supplier(sup)
                                .manufacturingDate(mfg)
                                .expiryDate(exp)
                                .quantity(inboundQty)
                                .status(BatchStatus.ACTIVE)
                                .build();
                        b = batchRepository.save(b);
                        simBatches.put(b.getBatchId(), b);

                        // Record Inbound transaction
                        transactionRepository.save(Transaction.builder()
                                .batch(b)
                                .type(TransactionType.INBOUND)
                                .quantity(inboundQty)
                                .timestamp(curDate.atTime(8, 0).atZone(clock.getZone()).toInstant())
                                .location("store room")
                                .createdBy("warehouse")
                                .build());
                    }

                    // Check for expired write-offs on curDate
                    for (Batch b : new ArrayList<>(simBatches.values())) {
                        if (b.getDrug().getDrugId().equals(drug.getDrugId())
                                && b.getStatus() == BatchStatus.ACTIVE
                                && !b.getExpiryDate().isAfter(curDate)
                                && b.getQuantity() > 0) {

                            transactionRepository.save(Transaction.builder()
                                    .batch(b)
                                    .type(TransactionType.EXPIRED_WRITE_OFF)
                                    .quantity(b.getQuantity())
                                    .timestamp(curDate.atTime(7, 30).atZone(clock.getZone()).toInstant())
                                    .location("store room")
                                    .createdBy("SYSTEM_DAILY_SCAN")
                                    .build());

                            b.setQuantity(0);
                            b.setStatus(BatchStatus.CLOSED);
                            batchRepository.save(b);
                        }
                    }

                    // Dispense (FEFO order among active unexpired batches)
                    List<Batch> dispBatches = simBatches.values().stream()
                            .filter(b -> b.getDrug().getDrugId().equals(drug.getDrugId())
                                    && b.getStatus() == BatchStatus.ACTIVE
                                    && b.getExpiryDate().isAfter(curDate)
                                    && b.getQuantity() > 0)
                            .sorted(Comparator.comparing(Batch::getExpiryDate))
                            .collect(Collectors.toList());

                    int remainingToDispense = groundTruth;
                    int dispensed = 0;

                    for (Batch b : dispBatches) {
                        if (remainingToDispense <= 0) break;
                        int take = Math.min(b.getQuantity(), remainingToDispense);
                        b.setQuantity(b.getQuantity() - take);
                        if (b.getQuantity() == 0) {
                            b.setStatus(BatchStatus.CLOSED);
                        }
                        batchRepository.save(b);

                        transactionRepository.save(Transaction.builder()
                                .batch(b)
                                .type(TransactionType.OUTBOUND)
                                .quantity(take)
                                .timestamp(curDate.atTime(14, 0).atZone(clock.getZone()).toInstant())
                                .location("dispensing counter")
                                .createdBy("pharmacist")
                                .build());

                        dispensed += take;
                        remainingToDispense -= take;
                    }

                    int unmet = groundTruth - dispensed;
                    if (writer != null) {
                        writer.printf("%s,%s,%d,%d,%d%n", curDate, drug.getDrugId(), unmet, dispensed, groundTruth);
                    }
                }
            }
        } finally {
            if (writer != null) {
                writer.close();
            }
        }

        // 4. Run risk evaluation across all drugs to populate initial snapshots and alerts
        riskEvaluationService.evaluateAllDrugs();
        log.info("Populated risk scores and alerts across {} drugs.", drugs.size());
    }
}
