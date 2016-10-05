package cloudwave.adaptationengine.plugins;
import cloudwave.adaptationengine.*;

import com.google.gson.Gson;
import java.util.*;
import java.text.SimpleDateFormat;

public class MigrateCongestedVMPlugin
  implements AdaptationEnginePlugin
{
  public AdaptationAction[] run(CloudWaveEvent event, AdaptationAction[] aa_list,
        Metrics metrics, Compute compute, Orchestration orchestration, Logger log)
  {
    String dest_compute_hostname = "";

    Gson gson = new Gson();

    // get 2 minutes ago
    String format = "yyyy-MM-dd'T'HH:mm:ss";
    SimpleDateFormat sdf = new SimpleDateFormat(format);
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    Calendar calendar = Calendar.getInstance();
    calendar.add(12, -2);
    String two_minutes_ago = sdf.format(calendar.getTime());

    // get list of samples for "host with least packetloss" from last couple of minutes
    // they are already sorted by sample time so grab the latest
    while(true){
	    String latest_least_packetloss_hosts = metrics.get("/v2/meters/host.least_packetloss?q.field=timestamp&q.op=gt&q.value=" + two_minutes_ago);
	    Sample[] least_packetloss_host_samples = (Sample[])gson.fromJson(latest_least_packetloss_hosts, Sample[].class);
	    // log.log("Least packetloss: " +  least_packetloss_host_samples[0].resource_id + " " + least_packetloss_host_samples[1].resource_id + " " + least_packetloss_host_samples[2].resource_id);
	    // try 3 times or keep going until timeout
	    if(Objects.equals(least_packetloss_host_samples[0].resource_id , least_packetloss_host_samples[1].resource_id) && 
	       Objects.equals(least_packetloss_host_samples[1].resource_id , least_packetloss_host_samples[2].resource_id))  {

		    // success! now update the engine
		    Sample destination_compute = least_packetloss_host_samples[0];
		    dest_compute_hostname = destination_compute.resource_id;
		    for (AdaptationAction action : aa_list) {
			    if (action.getType().equals(AdaptationType.MigrateAction)) {
				    log.log("AdaptationAction: Migrating vm: " + action.getTarget() + "into Host: " + dest_compute_hostname);
				    action.setDestination(dest_compute_hostname);
			    }
		    }
		    break;
	    }
    }

    return aa_list;
  }
}

class Data
{
    String vlanIP;
}

class Sample
{
    String counter_name;
    String user_id;
    String resource_id;
    String timestamp;
    String recorded_at;
    String message_id;
    String source;
    String counter_unit;
    double counter_volume;
    String project_id;
    ResourceMetadata resource_metadata;
    String counter_type;
}

class ResourceMetadata
{
    String event_type;
    String timestamp;
    String value;
    String source;
    String host;
    String name;
}
