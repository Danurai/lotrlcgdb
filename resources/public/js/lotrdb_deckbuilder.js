var _db_cards;
var _filter = {"pack_code":"Core","type_code":["hero","ally","attachment","event"]};  //treasure, player-side-quest 'objective ally'

$(document).ready(function () {
  
  $.getJSON('/api/data/cards',function (data) {
    data = data.filter(c => -1 < $.inArray(c.type_code, _filter.type_code));
    data = addzeroes(data,["attack","defense","willpower","threat"])
    _db_cards = TAFFY(data);
    write_table();
  });
  
  function write_table () {
    var cardtbl = $('#cardtbl')
    cardtbl.empty();
    _db_cards(_filter).each(c => cardtbl.append(tblrow(c)));
  }
  
  function tblrow (c) {
    return '<tr>'
      + '<td>+/-</td>'
      + '<td><a class="card-link" href="/card/' + c.code + '" data-code="' + c.code + '">' 
        + (c.is_unique ? '&bull;&nbsp;' : '')
        + c.name + '</a></td>'
      + '<td class="text-center">' + c.type_name + '</td>'
      + '<td class="text-center" title="' + c.sphere_name + '"><img class="icon-xs" src="/img/icons/sphere_' + c.sphere_code + '.png"</img></td>'
      + '<td class="text-center">' + (c.threat != -1 ? c.threat : (c.cost != -1 ? c.cost : "-"))+ '</td>'
      + '<td class="text-center">' + (c.attack != -1 ? c.attack : "-")+ '</td>'
      + '<td class="text-center">' + (c.defense != -1 ? c.defense : "-")+ '</td>'
      + '<td class="text-center">' + (c.willpower != -1 ? c.willpower : "-")+ '</td>'
      + '<td class="text-center">' + c.pack_code + ' #' + c.position + '</td>'
      + '</tr>';
  }  
  
  function addzeroes (data, fields) {
    var z = [];
    $.each(fields, function(k,v) {
      z[v]=-1
      data = data.map(c => $.extend({}, z, c));
    });
    return data;
  }
  
  
// FILTERS //
  function update_filter (k, v) {
    if (v.length == 0) { delete _filter[k]; } 
    else { _filter[k] = v; }
    write_table();
  }
    
  $('.btn-group').on('change',function() {
    update_filter (this.id, $(this).find('input:checked').map(function () { return $(this).val()}).get())
  });
  
});