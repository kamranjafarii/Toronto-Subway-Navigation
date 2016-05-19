package com.wifilocalizer.subwaynavigation;
public class JSON_ENUM {

	public enum JsonKeys{
        /* aps */mac(00),ssid(01),frequency(02),capabilities(11),date_created(21),date_modified(31),visit_count(10),last_visit(41),
        /* rps */rp_id(20),rp_name(51),rp_type(61),latitude(05),longitude(15),altitude(25),accuracy(35),floor_number(03),creator_id(30),building_id(251),
        /* aprp */rss(04),
        /* events */ event_id(50),start_date(71),end_date(81),start_time(91),end_time(101),event_title(111),short_info(121),long_info(131),media_id(06),
        /* users */ user_id(60),user_name(141),passwd(151),email(161),first_name(171),last_name(181),birth_date(191),
        /* control keys */ command_type(201),local_mac(70),response_type(221),error_code(231),message(241),aps(07);
        /* data types: long(%10=0) String(%10=1) int(%10=2) short(%10=3) byte(%10=04) double (%10=5) inputStream(%10=6) JSONarray (%10=7)*/
        private final int id;
        JsonKeys(int id) {this.id=id;}
        public JsonKeysTypes getKeyType()
        {
            return JsonKeysTypes.values()[this.id %10];
        }
    }

    
	public enum JsonKeysTypes{
        longtype,stringtype,inttype,shorttype, bytetype, doubletype, inputstreamtype, jsonarraytype
    }
    
    
    
    public enum CommandType{
        add_manual_rp,add_auto_rp,remove_rp,
        localize,get_close_rps,get_floor_rps,
        add_Event, remove_event, get_rp_events,
        update_rp,update_event,disconnet;
    }


    public enum ResponseType{manual_rp_added,auto_rp_added,rp_removed,
        location, close_rps, floor_rps, 
        event_added, event_removed, rp_events,
        rp_updated,event_updated,error;
    }


    public enum ErrorCode{insufficient_arguments,rp_not_found,rp_already_exists,
        rp_protected,no_Common_ap_found,event_not_found,
        event_already_exists,event_protected,unknown_error,
        localization_error,insufficient_privilages, db_error;
    }
}
