package verification_excel;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

public class Verification_Annuelle extends Verification {

    private int annee_actuelle;
	private HSSFWorkbook classeur_excel;
    private String chemin;
	private String[] onglets = {"Referentiel", "Fours", "Magnetoscopes", "Hifi"};
    private String[] referentiel_titres = {"Ville", "Action", "Pays", "Continent", "Devise", "Enseigne",
											"Publicite", "Region", "Emplacement", "Adresse", "Horaires_Ouverture",
											"Nb_Caisses", "Population", "Taux_Ouvrier", "Taux_Cadre", "Taux_Inactif",
											"Moins_25ans", "Les_25_35ans", "Plus_35ans"};
	private String[] materiel_titres = {"Ville", "Annee", "Mois", "O_Ventes", "O_CA", "O_MB"};
	
	Verification_Annuelle(String d) {
		chemin = d;
    }

	@Override
	public int Ouverture_fichier() {
		Date maDate = new Date();
		SimpleDateFormat maDateLongue = new SimpleDateFormat("yyyy");
		annee_actuelle = Integer.parseInt(maDateLongue.format(maDate).toString()); // Récupération de l'année
		String nom_fichier = "Darties_" + annee_actuelle;
		nom_fichier = nom_fichier + ".xls";
		chemin = chemin + nom_fichier;
		
		InputStream fic;
		try {
			// Ouverture fichier
			fic = new FileInputStream(chemin);
			try {
				// Initialisation du classeur
				classeur_excel = new HSSFWorkbook(new POIFSFileSystem(fic));
			}
			catch (IOException ex) {
				return 2; // Le fichier est corrumpu, ...
			}
		}
		catch (FileNotFoundException ex) {
			return 1; // Erreur sur l'ouverture du fichier : problème d'extension, chemin, ...
		}
		return 0;
    }

	@Override
    public int Verification_onglets() {
		for(int i = 0; i < onglets.length; i++) {
			Sheet feuille_actuelle = classeur_excel.getSheetAt(i); // Récupération de la feuille
			if (feuille_actuelle != null) {
				String nom_feuille_actuelle = feuille_actuelle.getSheetName(); // Récupération du nom de la feuille
				if(nom_feuille_actuelle.compareTo(onglets[i]) != 0) {
					return 3; // Le nom d'un onglet est incorrect
				}
			}
		}
		return 0;
     }
	
	@Override
    public int Verification_referentiel() {
        Sheet feuille1 = classeur_excel.getSheetAt(0);
		Row ligne1 = feuille1.getRow(0); // On se place sur la ligne1
		for(int colonne = 0; colonne < referentiel_titres.length; colonne++) {
			try {
				if(referentiel_titres[colonne].compareTo(ligne1.getCell(colonne).getStringCellValue()) != 0){
					return 5; // Erreur sur le titre d'une colonne
				}
			} catch (Exception e) {
				return 4; // Erreur sur le nombre de colonnes
			}
		}
		
        for(int i = 1; i < feuille1.getLastRowNum(); i++) {
			try {
				Row row = feuille1.getRow(i);
				String action = row.getCell(1).getStringCellValue();
				if(action.compareTo("A") != 0 && action.compareTo("M") != 0 && action.compareTo("S") != 0) {
					return 6; // Problème sur la donnée Action
				}
			} catch (Exception e) {
				return 7; // Probleme d'acces au contenu d'une cellule
			}
        }
        return 0;
     }

	@Override
    public int Verification_materiels() {
		 int colonne = 0;
		 int nombre_villes_BDD = 0;
		 int nombre_villes_a_ajouter = 0;
		 int nombre_villes_a_supprimer = 0;

		 // Connexion à la base de données et récupération du nombre de villes
		 try {
			// A FAIRE : Vérification que le nombre de lignes correspond bien à ce qu'il y a dans la BDD
			Connexion connexion = new Connexion();
			Connection cn = connexion.connection();
			Statement stat = cn.createStatement();
			String requete = "SELECT COUNT(idVille) As NbVilles FROM Ville";
			ResultSet resultat = stat.executeQuery(requete);
			while(resultat.next()) {
				nombre_villes_BDD = Integer.parseInt(resultat.getString(1));
			}
		} catch (SQLException ex) {
			return 13;
		}

		 Sheet feuille1 = classeur_excel.getSheetAt(0);
		 for(Row ligne : feuille1) {
			 if(ligne.getCell(1).getStringCellValue().compareTo("A") == 0) {
				 nombre_villes_a_ajouter++;
			 } else {
				 if(ligne.getCell(1).getStringCellValue().compareTo("S") == 0) {
					nombre_villes_a_supprimer++;
				 }
			 }
		 }
		 
		 // Nombre total de villes + ligne de titre
		 int nombre_total_lignes = 12*(nombre_villes_BDD+nombre_villes_a_ajouter-nombre_villes_a_supprimer) + 1;
		 //System.out.println(nombre_total_lignes);


		 // Parcours des trois feuilles concernant les objectifs
		 for (int index_onglet = 1; index_onglet < 4; index_onglet++) {
			 Sheet onglet_actuel = classeur_excel.getSheetAt(index_onglet); // Placement sur la feuille souhaitée

			 // Vérification du nombre de lignes
			 if(onglet_actuel.getLastRowNum() != nombre_total_lignes) {
				 return 14;
			 }

			 String ville_actuelle = onglet_actuel.getRow(1).getCell(0).getStringCellValue();
			 String ville_precedente = ville_actuelle;

			 int mois = 1;

			 for(Row ligne : onglet_actuel) {
				 if(ligne.getRowNum() ==  0) { // Si on est sur la première ligne, on vérifie les titres
					 for(colonne = 0; colonne < materiel_titres.length; colonne++) {
						 try {
							 if(materiel_titres[colonne].compareTo(ligne.getCell(colonne).getStringCellValue()) != 0){
								return 9; // Erreur sur le titre d'une colonne
							 }
						 } catch (Exception e) {
							 return 8; // Erreur sur le nombre de colonnes
						 }
					 }
				 } else { // On n'est pas sur la ligne de titre, on vérifie les données
					 try {
						 if(ligne.getCell(1).getNumericCellValue() != annee_actuelle) {
							 return 10; // L'année n'est pas correcte
						 }

						 ville_actuelle = ligne.getCell(0).getStringCellValue(); // Récupération de la ville courante
						 if (ville_actuelle.compareTo(ville_precedente) != 0) { // Nouvelle ville
							 mois = 1; // Le compteur de mois repart à 1
						 }
						 if (ligne.getCell(2).getNumericCellValue() != mois) { // Le mois n'est pas celui attendu
							 return 11; // Problème dans le nombre de mois
						 }

						 mois++;
						 ville_precedente = ligne.getCell(0).getStringCellValue();
					 } catch (Exception e) {
						 return 12; // Problème d'accès à une cellule
					 }
				  }
				  colonne = 0; // Remise à 0 de l'index pointant sur la colonne (pour les prochains onglets)
			 }
		 }
		 return 0;
     }

}
