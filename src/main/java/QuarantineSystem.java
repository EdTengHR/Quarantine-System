import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;


public class QuarantineSystem {
    public static class DashBoard {
        HashMap<String, Person> People;
        List<Integer> patientNums;
        List<Integer> infectNums;
        List<Double> infectAvgNums;
        List<Integer> vacNums;
        List<Integer> vacInfectNums;

        public DashBoard(HashMap<String, Person> p_People) {
            this.People = p_People;
            this.patientNums = new ArrayList<>(8);
            this.infectNums = new ArrayList<>(8);
            this.infectAvgNums = new ArrayList<>(8);
            this.vacNums = new ArrayList<>(8);
            this.vacInfectNums = new ArrayList<>(8);
        }

        public void runDashBoard() {
            for (int i = 0; i < 8; i++) {
                this.patientNums.add(0);
                this.infectNums.add(0);
                this.infectAvgNums.add(0.00);
                this.vacNums.add(0);
                this.vacInfectNums.add(0);
            }

            /*
             * TODO: Collect the statistics based on People
             *  Add the data in the lists, such as patientNums, infectNums, etc.
             */
            
            // Update data for each person within the people list
            for (Map.Entry<String, Person> entry : People.entrySet()) {
            	Person p = entry.getValue();
            	int age = p.getAge();
        		int index = age/10;
        		
        		Integer personInfectCnt = p.getInfectCnt();
        		Integer patientsAtAgeGrp = patientNums.get(index);
        		Integer infectionsAtAgeGrp = infectNums.get(index);
        		
        		// If person has been infected before, they are a patient
        		if (personInfectCnt > 0) {
        			patientNums.set(index, patientsAtAgeGrp + 1);
            		infectNums.set(index, infectionsAtAgeGrp + personInfectCnt);
            		
            		// Update the values of patientsAtAgeGrp and infectionsAtAgeGrp
            		patientsAtAgeGrp = patientNums.get(index);
            		infectionsAtAgeGrp = infectNums.get(index);
        		}
        		
        		if (p.getIsVac()) {
        			Integer vacAtAgeGrp = vacNums.get(index);
            		vacNums.set(index, vacAtAgeGrp + 1);
            		
            		if (personInfectCnt > 0) {
            			Integer vacInfectAtAgeGrp = vacInfectNums.get(index);
            			vacInfectNums.set(index, vacInfectAtAgeGrp + 1); 
            		}
        		}
        		
        		// Calculate average infections per patient (not person)
        		if (patientsAtAgeGrp > 0) {
        			Double infections = infectionsAtAgeGrp.doubleValue();
        			Double patients = patientsAtAgeGrp.doubleValue();
        			Double avgInfections = infections/patients;
        			infectAvgNums.set(index, avgInfections);
        		}
            }
        }
    }


    private HashMap<String, Person> People;
    private HashMap<String, Patient> Patients;

    private List<Record> Records;
    private List<Hospital> Hospitals;
    private int newHospitalNum;

    private DashBoard dashBoard;

    public QuarantineSystem() throws IOException {
        importPeople();
        importHospital();
        importRecords();
        dashBoard = null; 
        Patients = new HashMap<String, Patient>();
    }

    public void startQuarantine() throws IOException {
        /*
         * Task 1: Saving Patients
         */
        System.out.println("Task 1: Saving Patients");
        /*
         * TODO: Process each record
         */
        for (Record currRecord : Records) {
			Status status = currRecord.getStatus();
			
			if (status == Status.Confirmed) {
				Person recordPerson = (Person) People.get(currRecord.getIDCardNo());
				recordPerson.setInfectCnt(recordPerson.getInfectCnt() + 1);
				saveSinglePatient(currRecord);
			}
			else {
				
				releaseSinglePatient(currRecord);
			}
        }    	
        
        exportRecordTreatment();

        /*
         * Task 2: Displaying Statistics
         */
        System.out.println("Task 2: Displaying Statistics");
        dashBoard = new DashBoard(this.People);
        dashBoard.runDashBoard();
        exportDashBoard();
    }

    /*
     * Save a single patient when the status of the record is Confirmed
     */
    public void saveSinglePatient(Record record) {
        //TODO
    	Person p = (Person) People.get(record.getIDCardNo());
    	SymptomLevel sympLvl = record.getSymptomLevel();
		Patient patient = new Patient(p, sympLvl);
		patient.setInfectCnt(p.getInfectCnt());
		 
		// Find nearest hospital with appropriate symptom level cap
		List<Hospital> availHsptl = new ArrayList<>();
		for (Hospital hospital : this.Hospitals) {
			if (hospital.getCapacity().getSingleCapacity(sympLvl) > 0) {
				availHsptl.add(hospital);
			}
		}
		
		Hospital closestHospital = null;
		 
		// No available hospitals for the corresponding symptom level
		if (availHsptl.isEmpty()) {
			this.newHospitalNum += 1;
			String newHospitalID = "H-New-" + this.newHospitalNum;
			closestHospital = new Hospital(newHospitalID, patient.getLoc());
			this.Hospitals.add(closestHospital);
		}
		 
		int minDist = 0, count = 0;
		// This for loop doesn't run when availHsptl is empty
		for (Hospital hospital : availHsptl) {
			int dist = hospital.getLoc().getDisSquare(patient.getLoc());
			
			if (count == 0) {
				minDist = dist;
				closestHospital = hospital;
				count += 1;
			}
			else {
				if (dist < minDist) {
					minDist = dist;
					closestHospital = hospital;
				}
			}
		}
		String HID = closestHospital.HospitalID;
		patient.setHospitalID(HID);
		this.Patients.put(patient.getIDCardNo(), patient);
		
		System.out.println(patient.getIDCardNo() + " infect count = " + patient.getInfectCnt() + " " + patient.getHospitalID());
		
		// Handle hospital stuff
		closestHospital.addPatient(patient);
		closestHospital.getCapacity().decreaseCapacity(sympLvl);
		
		// Add in hospital id to the records list
		record.setHospitalID(HID);
    }

    /*
     * Release a single patient when the status of the record is Recovered
     */
    public void releaseSinglePatient(Record record) {
        //TODO    	
    	Patient patient = (Patient) Patients.get(record.getIDCardNo());
    	SymptomLevel sympLvl = record.getSymptomLevel();
		
		String HID = patient.getHospitalID();
		System.out.println(record.getIDCardNo() + HID);
		
		// Loop through hospitals to identify patient's hospital
		Hospital currHospital = null;
		for (Hospital h: Hospitals) {
			if (HID.equals(h.HospitalID)) {
				currHospital = h;
				break;
			}
		}
		
		// Handle hospital stuff
		currHospital.releasePatient(patient);
		currHospital.getCapacity().increaseCapacity(sympLvl);
		
		this.Patients.remove(patient.getHospitalID());
		
		// Add in hospital id to the records list
		record.setHospitalID(HID);
    }

    /*
     * Import the information of the people in the area from Person.txt
     * The data is finally stored in the attribute People
     * You do not need to change the method.
     */
    public void importPeople() throws IOException {
        this.People = new HashMap<String, Person>();
        File filename = new File("data/Person.txt");
        InputStreamReader reader = new InputStreamReader(new FileInputStream(filename));
        BufferedReader br = new BufferedReader(reader);
        String line = br.readLine();
        int lineNum = 0;

        while (line != null) {
            lineNum++;
            if (lineNum > 1) {
                String[] records = line.split("        ");
                assert (records.length == 6);
                String pIDCardNo = records[0];
                System.out.println(pIDCardNo);
                int XLoc = Integer.parseInt(records[1]);
                int YLoc = Integer.parseInt(records[2]);
                Location pLoc = new Location(XLoc, YLoc);
                assert (records[3].equals("Male") || records[3].equals("Female"));
                String pGender = records[3];
                int pAge = Integer.parseInt(records[4]);
                assert (records[5].equals("Yes") || records[5].equals("No"));
                boolean pIsVac = (records[5].equals("Yes"));
                Person p = new Person(pIDCardNo, pLoc, pGender, pAge, pIsVac);
                this.People.put(p.getIDCardNo(), p);
            }
            line = br.readLine();
        }
    }

    /*
     * Import the information of the records
     * The data is finally stored in the attribute Records
     * You do not need to change the method.
     */
    public void importRecords() throws IOException {
        this.Records = new ArrayList<>();

        File filename = new File("data/Record.txt");
        InputStreamReader reader = new InputStreamReader(new FileInputStream(filename));
        BufferedReader br = new BufferedReader(reader);
        String line = br.readLine();
        int lineNum = 0;

        while (line != null) {
            lineNum++;
            if (lineNum > 1) {
                String[] records = line.split("        ");
                assert(records.length == 3);
                String pIDCardNo = records[0];
                System.out.println(pIDCardNo);
                assert(records[1].equals("Critical") || records[1].equals("Moderate") || records[1].equals("Mild"));
                assert(records[2].equals("Confirmed") || records[2].equals("Recovered"));
                Record r = new Record(pIDCardNo, records[1], records[2]);
                Records.add(r);
            }
            line = br.readLine();
        }
    }

    /*
     * Import the information of the hospitals
     * The data is finally stored in the attribute Hospitals
     * You do not need to change the method.
     */
    public void importHospital() throws IOException {
        this.Hospitals = new ArrayList<>();
        this.newHospitalNum = 0;

        File filename = new File("data/Hospital.txt");
        InputStreamReader reader = new InputStreamReader(new FileInputStream(filename));
        BufferedReader br = new BufferedReader(reader);
        String line = br.readLine();
        int lineNum = 0;

        while (line != null) {
            lineNum++;
            if (lineNum > 1) {
                String[] records = line.split("        ");
                assert(records.length == 6);
                String pHospitalID = records[0];
                System.out.println(pHospitalID);
                int XLoc = Integer.parseInt(records[1]);
                int YLoc = Integer.parseInt(records[2]);
                Location pLoc = new Location(XLoc, YLoc);
                int pCritialCapacity = Integer.parseInt(records[3]);
                int pModerateCapacity = Integer.parseInt(records[4]);
                int pMildCapacity = Integer.parseInt(records[5]);
                Capacity cap = new Capacity(pCritialCapacity, pModerateCapacity, pMildCapacity);
                Hospital hospital = new Hospital(pHospitalID, pLoc, cap);
                this.Hospitals.add(hospital);
            }
            line = br.readLine();
        }
    }

    /*
     * Export the information of the records
     * The data is finally dumped into RecordTreatment.txt
     * DO NOT change the functionality of the method
     * Otherwise, you may generate wrong results in Task 1
     */
    public void exportRecordTreatment() throws IOException {
        File filename = new File("output/RecordTreatment.txt");
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(filename));
        BufferedWriter bw = new BufferedWriter(writer);
        bw.write("IDCardNo        SymptomLevel        Status        HospitalID\n");
        for (Record record : Records) {
            //Invoke the toString method of Record.
            bw.write(record.toString() + "\n");
        }
        bw.close();
    }

    /*
     * Export the information of the dashboard
     * The data is finally dumped into Statistics.txt
     * DO NOT change the functionality of the method
     * Otherwise, you may generate wrong results in Task 2
     */
    public void exportDashBoard() throws IOException {
        File filename = new File("output/Statistics.txt");
        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(filename));
        BufferedWriter bw = new BufferedWriter(writer);

        bw.write("AgeRange        patientNums        infectAvgNums        vacNums        vacInfectNums\n");

        for (int i = 0; i < 8; i++) {
            String ageRageStr = "";
            switch (i) {
                case 0:
                    ageRageStr = "(0, 10)";
                    break;
                case 7:
                    ageRageStr = "[70, infinite)";
                    break;
                default:
                    ageRageStr = "[" + String.valueOf(i) + "0, " + String.valueOf(i + 1) + "0)";
                    break;
            }
            String patientNumStr = String.valueOf(dashBoard.patientNums.get(i));
            String infectAvgNumsStr = String.valueOf(dashBoard.infectAvgNums.get(i));
            String vacNumsStr = String.valueOf(dashBoard.vacNums.get(i));
            String vacInfectNumsStr = String.valueOf(dashBoard.vacInfectNums.get(i));

            bw.write(ageRageStr + "        " + patientNumStr + "        " + infectAvgNumsStr + "        " + vacNumsStr + "        " + vacInfectNumsStr + "\n");
        }

        bw.close();
    }

    /* The entry of the project */
    public static void main(String[] args) throws IOException {
        QuarantineSystem system = new QuarantineSystem();
        System.out.println("Start Quarantine System");
        system.startQuarantine();
        System.out.println("Quarantine Finished");
    }
}
