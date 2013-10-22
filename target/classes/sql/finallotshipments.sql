SELECT DISTINCT wdd.lot_number  as lot_number
FROM apps.wsh_delivery_details wdd,
  apps.wsh_delivery_assignments wda,
  apps.hr_organization_units ou,
  apps.mtl_system_items msi,
  apps.wip_discrete_jobs wdj
WHERE wda.delivery_detail_id = wdd.delivery_detail_id
AND ou.organization_id       = wdd.organization_id
AND msi.inventory_item_id    = wdd.inventory_item_id
AND msi.organization_id      = wdd.organization_id
AND msi.item_type            = 'FG'
AND wdj.lot_number           = wdd.lot_number
AND wdj.organization_id     IN (113,114,115)
AND wdd.lot_number = '${in.body}'
AND NOT EXISTS
  (SELECT 1
	FROM apps.mtl_onhand_quantities moq
	WHERE wdd.lot_number = moq.lot_number
  )
ORDER BY lot_number 