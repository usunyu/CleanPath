package process;

import java.util.*;

import objects.*;
import function.*;

public class RDFOutputKMLGeneration {
	/**
	 * @param link
	 */
	static HashMap<Long, RDFLinkInfo> linkMap = new HashMap<Long, RDFLinkInfo>();
	/**
	 * @param node
	 */
	static HashMap<Long, RDFNodeInfo> nodeMap = new HashMap<Long, RDFNodeInfo>();
	/**
	 * @param sensor
	 */
	static HashMap<Integer, SensorInfo> matchSensorMap = new HashMap<Integer, SensorInfo>();
	static HashMap<Integer, SensorInfo> sensorMap = new HashMap<Integer, SensorInfo>();
	/**
	 * @param carpool
	 */
	static HashSet<Long> carpoolSet = new HashSet<Long>();
	/**
	 * carpool which not contained in database
	 */
	public static void manualCarpool() {
		carpoolSet.add(859176689l);
		carpoolSet.add(859176688l);
		carpoolSet.add(858795235l);
		carpoolSet.add(858795234l);
		carpoolSet.add(110161947l);
		carpoolSet.add(939442621l);
		carpoolSet.add(939442620l);
		carpoolSet.add(783652297l);
		carpoolSet.add(783652296l);
		carpoolSet.add(28432663l);
		carpoolSet.add(733916910l);
		carpoolSet.add(782774872l);
		carpoolSet.add(782774871l);
		carpoolSet.add(776739442l);
		carpoolSet.add(932209462l);
		carpoolSet.add(932209461l);
		carpoolSet.add(37825166l);
		carpoolSet.add(859175347l);
		carpoolSet.add(859175346l);
		carpoolSet.add(28432674l);
		carpoolSet.add(24612549l);
		carpoolSet.add(857627783l);
		carpoolSet.add(857627782l);
		carpoolSet.add(110160325l);
		carpoolSet.add(121237589l);
		carpoolSet.add(121234936l);
		carpoolSet.add(24612504l);
		carpoolSet.add(28432725l);
		carpoolSet.add(766693715l);
		carpoolSet.add(766693714l);
		carpoolSet.add(28432747l);
		carpoolSet.add(28432746l);
		carpoolSet.add(121238550l);
		carpoolSet.add(932222662l);
		carpoolSet.add(932222661l);
		carpoolSet.add(928343450l);
		carpoolSet.add(23928234l);
		carpoolSet.add(121238464l);
		carpoolSet.add(121238462l);
		carpoolSet.add(121234999l);
		carpoolSet.add(121238440l);
		carpoolSet.add(110162068l);
		carpoolSet.add(121240472l);
		carpoolSet.add(859174498l);
		carpoolSet.add(859174497l);
		carpoolSet.add(121241061l);
		carpoolSet.add(932209460l);
		carpoolSet.add(932209459l);
		carpoolSet.add(121235021l);
		carpoolSet.add(121235045l);
		carpoolSet.add(121235048l);
		carpoolSet.add(121235036l);
		carpoolSet.add(121235024l);
		carpoolSet.add(121233715l);
		carpoolSet.add(121241020l);
		carpoolSet.add(121241019l);
		carpoolSet.add(810665747l);
		carpoolSet.add(810665746l);
		carpoolSet.add(783167929l);
		carpoolSet.add(783167928l);
		carpoolSet.add(783167927l);
		carpoolSet.add(24559194l);
		carpoolSet.add(121240993l);
		carpoolSet.add(932219289l);
		carpoolSet.add(932219288l);
		carpoolSet.add(810665759l);
		carpoolSet.add(810665758l);
		carpoolSet.add(810862652l);
		carpoolSet.add(810862651l);
		carpoolSet.add(812029169l);
		carpoolSet.add(833175656l);
		carpoolSet.add(833175655l);
		carpoolSet.add(28433679l);
		carpoolSet.add(37825151l);
		carpoolSet.add(812029177l);
		carpoolSet.add(833250425l);
		carpoolSet.add(833250424l);
		carpoolSet.add(857627771l);
		carpoolSet.add(857627770l);
		carpoolSet.add(782793132l);
		carpoolSet.add(28433683l);
		carpoolSet.add(857627769l);
		carpoolSet.add(857627768l);
		carpoolSet.add(721218884l);
		carpoolSet.add(721218883l);
		carpoolSet.add(28434272l);
		carpoolSet.add(28434273l);
		carpoolSet.add(857765113l);
		carpoolSet.add(936892455l);
		carpoolSet.add(936892454l);
		carpoolSet.add(121240556l);
		carpoolSet.add(24159548l);
		carpoolSet.add(28433700l);
		carpoolSet.add(28433701l);
		carpoolSet.add(932219309l);
		carpoolSet.add(932219308l);
		carpoolSet.add(782793146l);
		carpoolSet.add(23927685l);
		carpoolSet.add(121233667l);
		carpoolSet.add(121233668l);
		carpoolSet.add(121233635l);
		carpoolSet.add(121234214l);
		carpoolSet.add(121234215l);
		carpoolSet.add(28433706l);
		carpoolSet.add(931101801l);
		carpoolSet.add(931101800l);
		carpoolSet.add(28433708l);
		carpoolSet.add(126510246l);
		carpoolSet.add(707425893l);
		carpoolSet.add(871479750l);
		carpoolSet.add(871479749l);
		carpoolSet.add(121234212l);
		carpoolSet.add(121234211l);
		carpoolSet.add(126510430l);
		carpoolSet.add(121234227l);
		carpoolSet.add(128810192l);
		carpoolSet.add(128810191l);
		carpoolSet.add(706846087l);
		carpoolSet.add(943507091l);
		carpoolSet.add(943507090l);
		carpoolSet.add(782730636l);
		carpoolSet.add(128810198l);
		carpoolSet.add(128810200l);
		carpoolSet.add(128810199l);
		carpoolSet.add(126510842l);
		carpoolSet.add(943510086l);
		carpoolSet.add(943510085l);
		
		carpoolSet.add(128785472l);
		carpoolSet.add(24612577l);
		carpoolSet.add(23927675l);
		carpoolSet.add(943506427l);
		carpoolSet.add(943506428l);
		carpoolSet.add(943506429l);
		carpoolSet.add(943506430l);
		carpoolSet.add(121234203l);
		carpoolSet.add(921307669l);
		carpoolSet.add(921307670l);
		carpoolSet.add(947343920l);
		carpoolSet.add(947343921l);
		carpoolSet.add(924939367l);
		carpoolSet.add(871479747l);
		carpoolSet.add(121234208l);
		carpoolSet.add(128798610l);
		carpoolSet.add(717341681l);
		carpoolSet.add(717341682l);
		carpoolSet.add(875087857l);
		carpoolSet.add(875087858l);
		carpoolSet.add(121234220l);
		carpoolSet.add(28433705l);
		carpoolSet.add(121234217l);
		carpoolSet.add(121234216l);
		carpoolSet.add(721106790l);
		carpoolSet.add(721106791l);
		carpoolSet.add(121233848l);
		carpoolSet.add(121233847l);
		carpoolSet.add(782793140l);
		carpoolSet.add(782793141l);
		carpoolSet.add(24154620l);
		carpoolSet.add(28433696l);
		carpoolSet.add(28433697l);
		carpoolSet.add(121240539l);
		carpoolSet.add(755956799l);
		carpoolSet.add(755956800l);
		carpoolSet.add(755956798l);
		carpoolSet.add(28433694l);
		carpoolSet.add(981741513l);
		carpoolSet.add(981741514l);
		carpoolSet.add(857627761l);
		carpoolSet.add(28434279l);
		carpoolSet.add(28434278l);
		carpoolSet.add(28484167l);
		carpoolSet.add(128789071l);
		carpoolSet.add(24159779l);
		carpoolSet.add(782793126l);
		carpoolSet.add(857627776l);
		carpoolSet.add(857627777l);
		carpoolSet.add(859172142l);
		carpoolSet.add(954895444l);
		carpoolSet.add(954895445l);
		carpoolSet.add(28433677l);
		carpoolSet.add(859172381l);
		carpoolSet.add(859174485l);
		carpoolSet.add(943679822l);
		carpoolSet.add(943679823l);
		carpoolSet.add(811173858l);
		carpoolSet.add(810864760l);
		carpoolSet.add(810665753l);
		carpoolSet.add(756632328l);
		carpoolSet.add(756632329l);
		carpoolSet.add(121240990l);
		carpoolSet.add(110162092l);
		carpoolSet.add(857627780l);
		carpoolSet.add(857627781l);
		carpoolSet.add(859174491l);
		carpoolSet.add(859174492l);
		carpoolSet.add(810665744l);
		carpoolSet.add(810665745l);
		carpoolSet.add(121241006l);
		carpoolSet.add(121241005l);
		carpoolSet.add(967817625l);
		carpoolSet.add(967817626l);
		carpoolSet.add(121235037l);
		carpoolSet.add(121235038l);
		carpoolSet.add(121235022l);
		carpoolSet.add(37825153l);
		carpoolSet.add(954816377l);
		carpoolSet.add(954816378l);
		carpoolSet.add(857627786l);
		carpoolSet.add(857627787l);
		carpoolSet.add(857710924l);
		carpoolSet.add(857710925l);
		carpoolSet.add(128798417l);
		carpoolSet.add(859186072l);
		carpoolSet.add(859186073l);
		carpoolSet.add(110162060l);
		carpoolSet.add(121235000l);
		carpoolSet.add(859186077l);
		carpoolSet.add(859186078l);
		carpoolSet.add(859186076l);
		carpoolSet.add(859186062l);
		carpoolSet.add(859186063l);
		carpoolSet.add(23928235l);
		carpoolSet.add(859186060l);
		carpoolSet.add(859186061l);
		carpoolSet.add(859186057l);
		carpoolSet.add(859186054l);
		carpoolSet.add(121238552l);
		carpoolSet.add(857627774l);
		carpoolSet.add(857627775l);
		carpoolSet.add(121238631l);
		carpoolSet.add(121238632l);
		carpoolSet.add(28432694l);
		carpoolSet.add(780472583l);
		carpoolSet.add(780472584l);
		carpoolSet.add(110160271l);
		carpoolSet.add(110160270l);
		carpoolSet.add(121238590l);
		carpoolSet.add(28432676l);
		carpoolSet.add(28432677l);
		carpoolSet.add(121238595l);
		carpoolSet.add(121237576l);
		carpoolSet.add(37825165l);
		carpoolSet.add(37825160l);
		carpoolSet.add(783086689l);
		carpoolSet.add(783086690l);
		carpoolSet.add(782774874l);
		carpoolSet.add(733916908l);
		carpoolSet.add(781854669l);
		carpoolSet.add(788181078l);
		carpoolSet.add(788181079l);
		carpoolSet.add(23842747l);
		carpoolSet.add(110161949l);
		carpoolSet.add(940006088l);
		carpoolSet.add(940006089l);
	}
	
	public static void main(String[] args) {
		
		RDFInput.readNodeFile(nodeMap);
		RDFOutput.generateNodeKML(nodeMap);
		
		RDFInput.readLinkFile(linkMap, nodeMap);
		RDFInput.readLinkGeometry(linkMap);
		RDFInput.readLinkLane(linkMap);
		
		RDFInput.fetchSensor(sensorMap);
		RDFInput.readMatchSensor(linkMap, sensorMap, matchSensorMap);
		RDFOutput.generateSensorKML(matchSensorMap);
		
		RDFOutput.generateLinkKML(linkMap);
	}
}
